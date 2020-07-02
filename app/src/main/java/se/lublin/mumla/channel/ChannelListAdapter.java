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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import se.lublin.humla.HumlaService;
import se.lublin.humla.IHumlaService;
import se.lublin.humla.IHumlaSession;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.model.IUser;
import se.lublin.humla.model.Server;
import se.lublin.humla.model.TalkState;
import se.lublin.mumla.Constants;
import se.lublin.mumla.R;
import se.lublin.mumla.db.MumlaDatabase;
import se.lublin.mumla.drawable.CircleDrawable;
import se.lublin.mumla.service.MumlaService;

/**
 * Created by andrew on 31/07/13.
 */
public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements UserMenu.IUserLocalStateListener {
    // Set particular bits to make the integer-based model item ids unique.
    public static final long CHANNEL_ID_MASK = (0x1L << 32);
    public static final long USER_ID_MASK = (0x1L << 33);

    private Context mContext;
    private IHumlaService mService;
    private MumlaDatabase mDatabase;
    private List<Integer> mRootChannels;
    private List<Node> mNodes;
    /**
     * A mapping of user-set channel expansions.
     * If a key is not mapped, default to hiding empty channels.
     */
    private HashMap<Integer, Boolean> mExpandedChannels;
    private OnUserClickListener mUserClickListener;
    private OnChannelClickListener mChannelClickListener;
    private boolean mShowChannelUserCount;
    private final FragmentManager mFragmentManager;

    public ChannelListAdapter(Context context, IHumlaService service, MumlaDatabase database,
                              FragmentManager fragmentManager, boolean showPinnedOnly,
                              boolean showChannelUserCount) throws RemoteException {
        setHasStableIds(true);
        mContext = context;
        mService = service;
        mDatabase = database;
        mFragmentManager = fragmentManager;
        mShowChannelUserCount = showChannelUserCount;

        mRootChannels = new ArrayList<Integer>();
        if(showPinnedOnly) {
            mRootChannels = mDatabase.getPinnedChannels(mService.getTargetServer().getId());
        } else {
            mRootChannels.add(0);
        }

        // Construct channel tree
        mNodes = new LinkedList<Node>();
        mExpandedChannels = new HashMap<Integer, Boolean>();
        updateChannels();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(viewType, viewGroup, false);
        if (viewType == R.layout.channel_row) {
            return new ChannelViewHolder(view);
        } else if (viewType == R.layout.channel_user_row) {
            return new UserViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        final Node node = mNodes.get(position);
        if (node.isChannel()) {
            final IChannel channel = node.getChannel();
            final ChannelViewHolder cvh = (ChannelViewHolder) viewHolder;
            cvh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mChannelClickListener != null) {
                        mChannelClickListener.onChannelClick(channel);
                    }
                }
            });

            final boolean expandUsable = channel.getSubchannels().size() > 0 ||
                    channel.getSubchannelUserCount() > 0;
            cvh.mChannelExpandToggle.setImageResource(node.isExpanded() ?
                    R.drawable.ic_action_expanded : R.drawable.ic_action_collapsed);
            cvh.mChannelExpandToggle.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mExpandedChannels.put(channel.getId(), !node.isExpanded());
                    updateChannels(); // FIXME: very inefficient.
                    notifyDataSetChanged();
                }
            });
            // Dim channel expand toggle when no subchannels exist
            cvh.mChannelExpandToggle.setEnabled(expandUsable);
            cvh.mChannelExpandToggle.setVisibility(expandUsable ? View.VISIBLE : View.INVISIBLE);

            cvh.mChannelName.setText(channel.getName());

            int nameTypeface = Typeface.NORMAL;
            if (mService != null && mService.isConnected()) {
                IHumlaSession session = mService.HumlaSession();
                IChannel ourChan = null;
                try {
                    ourChan = session.getSessionChannel();
                } catch(IllegalStateException e) {
                    Log.d(Constants.TAG, "ChannelListAdapter, exception in onBindViewHolder: " + e);
                }
                if (ourChan != null) {
                    if (channel.equals(ourChan)) {
                        nameTypeface |= Typeface.BOLD;
                        // Always italicize our current channel if it has a link.
                        if (channel.getLinks().size() > 0) {
                            nameTypeface |= Typeface.ITALIC;
                        }
                    }
                    // Italicize channels in a link with our current channel.
                    if (channel.getLinks().contains(ourChan)) {
                        nameTypeface |= Typeface.ITALIC;
                    }
                }
            }
            cvh.mChannelName.setTypeface(null, nameTypeface);

            if (mShowChannelUserCount) {
                cvh.mChannelUserCount.setVisibility(View.VISIBLE);
                int userCount = channel.getSubchannelUserCount();
                cvh.mChannelUserCount.setText(String.format("%d", userCount));
            } else {
                cvh.mChannelUserCount.setVisibility(View.GONE);
            }

            // Pad the view depending on channel's nested level.
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            float margin = node.getDepth() * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, metrics);
            cvh.mChannelHolder.setPadding((int) margin,
                    cvh.mChannelHolder.getPaddingTop(),
                    cvh.mChannelHolder.getPaddingRight(),
                    cvh.mChannelHolder.getPaddingBottom());

            cvh.mJoinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService.isConnected())
                        mService.HumlaSession().joinChannel(channel.getId());
                }
            });

            cvh.mMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChannelMenu menu = new ChannelMenu(mContext, channel, mService, mDatabase, mFragmentManager);
                    menu.showPopup(v);
                }
            });

            cvh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    cvh.mMoreButton.performClick();
                    return true;
                }
            });
        } else if (node.isUser()) {
            final IUser user = node.getUser();
            final UserViewHolder uvh = (UserViewHolder) viewHolder;
            uvh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mUserClickListener != null) {
                        mUserClickListener.onUserClick(user);
                    }
                }
            });

            uvh.mUserName.setText(user.getName());

            final int typefaceStyle;
            if (mService.isConnected() && mService.HumlaSession().getSessionId() == user.getSession()) {
                typefaceStyle = Typeface.BOLD;
            } else {
                typefaceStyle = Typeface.NORMAL;
            }
            uvh.mUserName.setTypeface(null, typefaceStyle);

            uvh.mUserTalkHighlight.setImageDrawable(getTalkStateDrawable(user));

            // Pad the view depending on channel's nested level.
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            float margin = (node.getDepth() + 1) * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, metrics);
            uvh.mUserHolder.setPadding((int) margin,
                    uvh.mUserHolder.getPaddingTop(),
                    uvh.mUserHolder.getPaddingRight(),
                    uvh.mUserHolder.getPaddingBottom());

            uvh.mMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UserMenu menu = new UserMenu(mContext, user, (MumlaService) mService,
                            mFragmentManager, ChannelListAdapter.this);
                    menu.showPopup(v);
                }
            });

            uvh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    uvh.mMoreButton.performClick();
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mNodes.size();
    }

    @Override
    public int getItemViewType(int position) {
        Node node = mNodes.get(position);
        if (node.isChannel()) {
            return R.layout.channel_row;
        } else if (node.isUser()) {
            return R.layout.channel_user_row;
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        try {
            return mNodes.get(position).getId();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Updates the channel tree model.
     * To be used after any channel tree modifications.
     */
    public void updateChannels() {
        if (!mService.isConnected())
            return;

        IHumlaSession session = mService.HumlaSession();
        mNodes.clear();
        try {
            for (int cid : mRootChannels) {
                IChannel channel = session.getChannel(cid);
                if (channel != null) {
                    constructNodes(null, channel, 0, mNodes);
                }
            }
        } catch (IllegalStateException e) {
            Log.d(Constants.TAG, "ChannelListAdapter, exception in updateChannels: " + e);
        }
    }

    /**
     * Updates the user's state icon with a nice animation.
     * @param user The user to update.
     * @param view The view containing this adapter.
     */
    public void animateUserStateUpdate(IUser user, RecyclerView view) {
        long itemId = user.getSession() | USER_ID_MASK;
        UserViewHolder uvh = (UserViewHolder) view.findViewHolderForItemId(itemId);
        if (uvh != null) {
            Drawable newState = getTalkStateDrawable(user);
            if (!uvh.mUserTalkHighlight.getDrawable().getCurrent().getConstantState()
                    .equals(newState.getConstantState())) {
                uvh.mUserTalkHighlight.setImageDrawable(newState);
            }
        }
    }

    private Drawable getTalkStateDrawable(IUser user) {
        Resources resources = mContext.getResources();
        if (user.isSelfDeafened()) {
            return resources.getDrawable(R.drawable.outline_circle_deafened);
        } else if (user.isDeafened()) {
            return resources.getDrawable(R.drawable.outline_circle_server_deafened);
        } else if (user.isSelfMuted()) {
            return resources.getDrawable(R.drawable.outline_circle_muted);
        } else if (user.isMuted()) {
            return resources.getDrawable(R.drawable.outline_circle_server_muted);
        } else if (user.isSuppressed()) {
            return resources.getDrawable(R.drawable.outline_circle_suppressed);
        } else if (user.getTalkState() == TalkState.TALKING
                || user.getTalkState() == TalkState.SHOUTING
                || user.getTalkState() == TalkState.WHISPERING) {
            // TODO whisper and shouting?
            return resources.getDrawable(R.drawable.outline_circle_talking_on);
        } else {
            // Passive drawables
            if (user.getTexture() != null) {
                // FIXME: cache bitmaps
                Bitmap bitmap = BitmapFactory.decodeByteArray(user.getTexture(), 0, user.getTexture().length);
                // yes, decoding can fail
                if (bitmap != null) {
                    return new CircleDrawable(mContext.getResources(), bitmap);
                }
            }
        }
        // "default" symbol, used also if bitmap decoding fails
        return resources.getDrawable(R.drawable.outline_circle_talking_off);
    }

    public int getUserPosition(int session) {
        long itemId = session | USER_ID_MASK;
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            try {
                if (node.getId() == itemId) {
                    return i;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public int getChannelPosition(int channelId) {
        long itemId = channelId | CHANNEL_ID_MASK;
        for (int i = 0; i < mNodes.size(); i++) {
            Node node = mNodes.get(i);
            try {
                if (node.getId() == itemId) {
                    return i;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        mUserClickListener = listener;
    }

    public void setOnChannelClickListener(OnChannelClickListener listener) {
        mChannelClickListener = listener;
    }

    /**
     * Sets whether to show the channel user count in a channel row.
     */
    public void setShowChannelUserCount(boolean showUserCount) {
        mShowChannelUserCount = showUserCount;
        notifyDataSetChanged();
    }

    /**
     * Recursively creates a list of {@link Node}s representing the channel hierarchy.
     * @param parent The parent node to propagate under.
     * @param channel The parent channel.
     * @param depth The current depth of the subtree.
     * @param nodes An accumulator to store generated nodes into.
     */
    private void constructNodes(Node parent, IChannel channel, int depth,
                                List<Node> nodes) {
        Node channelNode = new Node(parent, depth, channel);
        nodes.add(channelNode);

        Boolean expandSetting = mExpandedChannels.get(channel.getId());
        if ((expandSetting == null && channel.getSubchannelUserCount() == 0)
                || (expandSetting != null && !expandSetting)) {
            channelNode.setExpanded(false);
            return; // Skip adding children of contracted/empty channels.
        }

        for (IUser user : channel.getUsers()) {
            if (user == null) {
                continue;
            }
            nodes.add(new Node(channelNode, depth, user));
        }
        for (IChannel subc : channel.getSubchannels()) {
            constructNodes(channelNode, subc, depth + 1, nodes);
        }
    }

    /**
     * Changes the service backing the adapter. Updates the list as well.
     * @param service The new service to retrieve channels from.
     */
    public void setService(IHumlaService service) {
        mService = service;
        if (service.getConnectionState() == HumlaService.ConnectionState.CONNECTED) {
            updateChannels();
            notifyDataSetChanged();
        }
    }

    @Override
    public void onLocalUserStateUpdated(final IUser user) {
        notifyDataSetChanged();

        // Add or remove registered user from local mute history
        final Server server = mService.getTargetServer();

        if (user.getUserId() >= 0 && server.isSaved()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (user.isLocalMuted()) {
                        mDatabase.addLocalMutedUser(server.getId(), user.getUserId());
                    } else {
                        mDatabase.removeLocalMutedUser(server.getId(), user.getUserId());
                    }
                    if (user.isLocalIgnored()) {
                        mDatabase.addLocalIgnoredUser(server.getId(), user.getUserId());
                    } else {
                        mDatabase.removeLocalIgnoredUser(server.getId(), user.getUserId());
                    }
                }
            }).start();
        }
    }

    private static class UserViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout mUserHolder;
        public TextView mUserName;
//        public ImageView mUserAvatar;
        public ImageView mUserTalkHighlight;
        public ImageView mMoreButton;

        public UserViewHolder(View itemView) {
            super(itemView);
            mUserHolder = (LinearLayout) itemView.findViewById(R.id.user_row_title);
            mUserTalkHighlight = (ImageView) itemView.findViewById(R.id.user_row_talk_highlight);
            mUserName = (TextView) itemView.findViewById(R.id.user_row_name);
            mMoreButton = (ImageView) itemView.findViewById(R.id.user_row_more);
        }
    }

    private static class ChannelViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout mChannelHolder;
        public ImageView mChannelExpandToggle;
        public TextView mChannelName;
        public TextView mChannelUserCount;
        public ImageView mJoinButton;
        public ImageView mMoreButton;

        public ChannelViewHolder(View itemView) {
            super(itemView);
            mChannelHolder = (LinearLayout) itemView.findViewById(R.id.channel_row_title);
            mChannelExpandToggle = (ImageView) itemView.findViewById(R.id.channel_row_expand);
            mChannelName = (TextView) itemView.findViewById(R.id.channel_row_name);
            mChannelUserCount = (TextView) itemView.findViewById(R.id.channel_row_count);
            mJoinButton = (ImageView) itemView.findViewById(R.id.channel_row_join);
            mMoreButton = (ImageView) itemView.findViewById(R.id.channel_row_more);
        }
    }

    /**
     * An arbitrary node in the channel-user hierarchy.
     * Can be either a channel or user.
     */
    private static class Node {
        private Node mParent;
        private IChannel mChannel;
        private IUser mUser;
        private int mDepth;
        private boolean mExpanded;

        public Node(Node parent, int depth, IChannel channel) {
            mParent = parent;
            mChannel = channel;
            mDepth = depth;
            mExpanded = true;
        }

        public Node(Node parent, int depth, IUser user) {
            mParent = parent;
            mUser = user;
            mDepth = depth;
        }

        public boolean isChannel() {
            return mChannel != null;
        }

        public boolean isUser() {
            return mUser != null;
        }

        public Node getParent() {
            return mParent;
        }

        public IChannel getChannel() {
            return mChannel;
        }

        public IUser getUser() {
            return mUser;
        }

        public Long getId() throws RemoteException {
            // Apply flags to differentiate integer-length identifiers
            if (isChannel()) {
                return CHANNEL_ID_MASK | mChannel.getId();
            } else if (isUser()) {
                return USER_ID_MASK | mUser.getSession();
            }
            return null;
        }

        public int getDepth() {
            return mDepth;
        }

        public boolean isExpanded() {
            return mExpanded;
        }

        public void setExpanded(boolean expanded) {
            mExpanded = expanded;
        }
    }
}
