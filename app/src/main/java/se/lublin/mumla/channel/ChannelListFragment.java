/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.lublin.mumla.channel;

import static android.content.Context.RECEIVER_NOT_EXPORTED;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.CursorWrapper;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import se.lublin.humla.IHumlaService;
import se.lublin.humla.IHumlaSession;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.model.IUser;
import se.lublin.humla.util.HumlaDisconnectedException;
import se.lublin.humla.util.HumlaException;
import se.lublin.humla.util.HumlaObserver;
import se.lublin.humla.util.IHumlaObserver;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;
import se.lublin.mumla.db.DatabaseProvider;
import se.lublin.mumla.util.HumlaServiceFragment;

public class ChannelListFragment extends HumlaServiceFragment implements OnChannelClickListener, OnUserClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = ChannelListFragment.class.getName();

    private IHumlaObserver mServiceObserver = new HumlaObserver() {
        @Override
        public void onDisconnected(HumlaException e) {
            mChannelView.setAdapter(null);
        }

        @Override
        public void onUserJoinedChannel(IUser user, IChannel newChannel, IChannel oldChannel) {
            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();

            if (getService() == null || !getService().isConnected()) {
                return;
            }

            int selfSession;
            try {
                selfSession = getService().HumlaSession().getSessionId();
            } catch (HumlaDisconnectedException|IllegalStateException e) {
                Log.d(TAG, "exception in onUserJoinedChannel: " + e);
                return;
            }

            if (user.getSession() == selfSession) {
                scrollToChannel(newChannel.getId());
            }
        }

        @Override
        public void onChannelAdded(IChannel channel) {
            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChannelRemoved(IChannel channel) {
            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChannelStateUpdated(IChannel channel) {
            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onUserConnected(IUser user) {
            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onUserRemoved(IUser user, String reason) {
            // If we are the user being removed, don't update the channel list.
            // We won't be in a synchronized state.
            if (getService() == null || !getService().isConnected()) {
                return;
            }

            mChannelListAdapter.updateChannels();
            mChannelListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onUserStateUpdated(IUser user) {
            mChannelListAdapter.updateUserStates(user, mChannelView);
            getActivity().supportInvalidateOptionsMenu(); // Update self mute/deafen state
        }

        @Override
        public void onUserTalkStateUpdated(IUser user) {
            mChannelListAdapter.updateUserStates(user, mChannelView);
        }
    };

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(getActivity() != null)
                getActivity().supportInvalidateOptionsMenu(); // Update bluetooth menu item
        }
    };

    private RecyclerView mChannelView;
    private ChannelListAdapter mChannelListAdapter;
    private ChatTargetProvider mTargetProvider;
    private DatabaseProvider mDatabaseProvider;
    private ActionMode mActionMode;
    private Settings mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mTargetProvider = (ChatTargetProvider) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()+" must implement ChatTargetProvider");
        }
        try {
            mDatabaseProvider = (DatabaseProvider) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()+" must implement DatabaseProvider");
        }
        mSettings = Settings.getInstance(activity);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_list, container, false);
        mChannelView = (RecyclerView) view.findViewById(R.id.channelUsers);
        mChannelView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(mChannelView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getActivity().registerReceiver(mBluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED), RECEIVER_NOT_EXPORTED);
        } else {
            getActivity().registerReceiver(mBluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
        }
    }

    @Override
    public void onDetach() {
        getActivity().unregisterReceiver(mBluetoothReceiver);
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IHumlaObserver getServiceObserver() {
        return mServiceObserver;
    }

    @Override
    public void onServiceBound(IHumlaService service) {
        try {
            if (mChannelListAdapter == null) {
                setupChannelList();
            } else {
                mChannelListAdapter.setService(service);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem muteItem = menu.findItem(R.id.menu_mute_button);
        MenuItem deafenItem = menu.findItem(R.id.menu_deafen_button);

        if(getService() != null && getService().isConnected()) {
            IHumlaSession session = getService().HumlaSession();

            // Color the action bar icons to the primary text color of the theme, TODO move this elsewhere
            int foregroundColor = getActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimaryInverse}).getColor(0, -1);

            IUser self = session.getSessionUser();
            if (self != null) {
                muteItem.setIcon(self.isSelfMuted() ? R.drawable.ic_action_microphone_muted : R.drawable.ic_action_microphone);
                deafenItem.setIcon(self.isSelfDeafened() ? R.drawable.ic_action_audio_muted : R.drawable.ic_action_audio);
                muteItem.getIcon().mutate().setColorFilter(foregroundColor, PorterDuff.Mode.MULTIPLY);
                deafenItem.getIcon().mutate().setColorFilter(foregroundColor, PorterDuff.Mode.MULTIPLY);
            }

            MenuItem bluetoothItem = menu.findItem(R.id.menu_bluetooth);
            bluetoothItem.setChecked(session.usingBluetoothSco());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_channel_list, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView)MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int i) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int i) {
                if (getService() == null || !getService().isConnected())
                    return false;
                CursorWrapper cursor = (CursorWrapper) searchView.getSuggestionsAdapter().getItem(i);
                int typeColumn = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
                int dataIdColumn = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA);
                String itemType = cursor.getString(typeColumn);
                int itemId = cursor.getInt(dataIdColumn);

                IHumlaSession session = getService().HumlaSession();
                if(ChannelSearchProvider.INTENT_DATA_CHANNEL.equals(itemType)) {
                    if(session.getSessionChannel().getId() != itemId) {
                        session.joinChannel(itemId);
                    } else {
                        scrollToChannel(itemId);
                    }
                    return true;
                } else if(ChannelSearchProvider.INTENT_DATA_USER.equals(itemType)) {
                    scrollToUser(itemId);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (getService() == null || !getService().isConnected())
            return super.onOptionsItemSelected(item);

        IHumlaSession session = getService().HumlaSession();
        int itemId = item.getItemId();
        if (itemId == R.id.menu_mute_button) {
            IUser self = session.getSessionUser();
            if (self != null) {
                boolean muted = !self.isSelfMuted();
                boolean deafened = self.isSelfDeafened();
                deafened &= muted; // Undeafen if mute is off
                session.setSelfMuteDeafState(muted, deafened);
            }

            getActivity().supportInvalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.menu_deafen_button) {
            IUser self = session.getSessionUser();
            if (self != null) {
                boolean deafened = !self.isSelfDeafened();
                session.setSelfMuteDeafState(deafened, deafened);
            }

            getActivity().supportInvalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.menu_search) {
            return false;
        } else if (itemId == R.id.menu_bluetooth) {
            item.setChecked(!item.isChecked());
            if (item.isChecked()) {
                session.enableBluetoothSco();
            } else {
                session.disableBluetoothSco();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupChannelList() throws RemoteException {
        mChannelListAdapter = new ChannelListAdapter(getActivity(), getService(),
                mDatabaseProvider.getDatabase(), getChildFragmentManager(),
                isShowingPinnedChannels(), mSettings.shouldShowUserCount());
        mChannelListAdapter.setOnChannelClickListener(this);
        mChannelListAdapter.setOnUserClickListener(this);
        mChannelView.setAdapter(mChannelListAdapter);
        mChannelListAdapter.notifyDataSetChanged();
    }

    /**
     * Scrolls to the passed channel.
     */
    public void scrollToChannel(int channelId) {
        int channelPosition = mChannelListAdapter.getChannelPosition(channelId);
        mChannelView.scrollToPosition(channelPosition);
    }
    /**
     * Scrolls to the passed user.
     */
    public void scrollToUser(int userId) {
        int userPosition = mChannelListAdapter.getUserPosition(userId);
        mChannelView.scrollToPosition(userPosition);
    }

    private boolean isShowingPinnedChannels() {
        return getArguments().getBoolean("pinned");
    }
    @Override
    public void onChannelClick(IChannel channel) {
        if (mTargetProvider.getChatTarget() != null &&
                channel.equals(mTargetProvider.getChatTarget().getChannel()) &&
                mActionMode != null) {
            // Dismiss action mode if double pressed. FIXME: use list view selection instead?
            mActionMode.finish();
        } else {
            ActionMode.Callback cb = new ChatTargetActionModeCallback(mTargetProvider, new ChatTargetProvider.ChatTarget(channel)) {
                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    super.onDestroyActionMode(actionMode);
                    mActionMode = null;
                }
            };
            mActionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(cb);
        }
    }

    @Override
    public void onUserClick(IUser user) {
        if (mTargetProvider.getChatTarget() != null &&
                user.equals(mTargetProvider.getChatTarget().getUser()) &&
                mActionMode != null) {
            // Dismiss action mode if double pressed. FIXME: use list view selection instead?
            mActionMode.finish();
        } else {
            ActionMode.Callback cb = new ChatTargetActionModeCallback(mTargetProvider, new ChatTargetProvider.ChatTarget(user)) {
                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    super.onDestroyActionMode(actionMode);
                    mActionMode = null;
                }
            };
            mActionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(cb);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.PREF_SHOW_USER_COUNT.equals(key) && mChannelListAdapter != null) {
            mChannelListAdapter.setShowChannelUserCount(mSettings.shouldShowUserCount());
        }
    }
}
