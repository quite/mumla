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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import se.lublin.humla.IHumlaSession;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.net.Permissions;
import se.lublin.mumla.R;
import se.lublin.mumla.util.HumlaServiceProvider;

/**
 * Created by andrew on 23/11/13.
 */
public class ChannelEditFragment extends DialogFragment {

    private HumlaServiceProvider mServiceProvider;
    private TextView mNameField;
    private TextView mDescriptionField;
    private TextView mPositionField;
    private CheckBox mTemporaryBox;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mServiceProvider = (HumlaServiceProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement HumlaServiceProvider");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.fragment_channel_edit, null, false);
        mNameField = (TextView) view.findViewById(R.id.channel_edit_name);
        mDescriptionField = (TextView) view.findViewById(R.id.channel_edit_description);
        mPositionField = (TextView) view.findViewById(R.id.channel_edit_position);
        mTemporaryBox = (CheckBox) view.findViewById(R.id.channel_edit_temporary);

        // If we can only make temporary channels, remove the option.
        if (mServiceProvider.getService().isConnected()) {
            // TODO: we probably should just stop this dialog in its tracks if we're disconnected.
            IHumlaSession session = mServiceProvider.getService().HumlaSession();
            IChannel parentChannel = session.getChannel(getParent());
            int combinedPermissions = session.getPermissions() | parentChannel.getPermissions();
            boolean canMakeChannel = (combinedPermissions & Permissions.MakeChannel) > 0;
            boolean canMakeTempChannel = (combinedPermissions & Permissions.MakeTempChannel) > 0;
            boolean onlyTemp = canMakeTempChannel && !canMakeChannel;
            mTemporaryBox.setChecked(onlyTemp);
            mTemporaryBox.setEnabled(!onlyTemp);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(isAdding() ? R.string.channel_add : R.string.channel_edit)
                .setView(view)
                .setPositiveButton(isAdding() ? R.string.add : R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(isAdding() && mServiceProvider.getService().isConnected()) {
                            mServiceProvider.getService().HumlaSession().createChannel(getParent(),
                                    mNameField.getText().toString(),
                                    mDescriptionField.getText().toString(),
                                    Integer.parseInt(mPositionField.getText().toString()), // We can guarantee this to be an int. InputType is numberSigned.
                                    mTemporaryBox.isChecked());
                        } else {
                            // TODO
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    /**
     * @return true if the user is adding a new channel.
     */
    public boolean isAdding() {
        return getArguments().getBoolean("adding");
    }

    /**
     * @return the parent channel that the new channel will be a child of.
     */
    public int getParent() {
        return getArguments().getInt("parent");
    }
    /**
     * @return the channel being updated.
     */
    public int getChannel() {
        return getArguments().getInt("channel");
    }
}
