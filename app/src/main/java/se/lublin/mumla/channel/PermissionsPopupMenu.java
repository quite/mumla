/*
 * Copyright (C) 2015 Andrew Comminos <andrew@comminos.com>
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
import android.view.Menu;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;

import se.lublin.humla.IHumlaService;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.util.HumlaObserver;
import se.lublin.humla.util.IHumlaObserver;

/**
 * Encapsulates a menu requiring permissions.
 * Created by andrew on 19/11/15.
 */
public class PermissionsPopupMenu implements PopupMenu.OnDismissListener {
    private final Context mContext;
    private final IChannel mChannel;
    private final IHumlaService mService;
    private final PopupMenu mMenu;
    private final IOnMenuPrepareListener mPrepareListener;

    private final IHumlaObserver mPermissionsObserver = new HumlaObserver() {
        @Override
        public void onChannelPermissionsUpdated(IChannel channel) {
            if (mChannel.equals(channel)) {
                mPrepareListener.onMenuPrepare(mMenu.getMenu(), getPermissions());
            }
        }
    };

    public PermissionsPopupMenu(Context context, View anchor, int menuRes,
                                IOnMenuPrepareListener enforcer,
                                PopupMenu.OnMenuItemClickListener itemClickListener,
                                IChannel channel, IHumlaService service) {
        mContext = context;
        mChannel = channel;
        mService = service;
        mPrepareListener = enforcer;
        mMenu = new PopupMenu(mContext, anchor);
        mMenu.inflate(menuRes);
        mMenu.setOnDismissListener(this);
        mMenu.setOnMenuItemClickListener(itemClickListener);
    }

    private int getPermissions() {
        if (mService.isConnected()) {
            return mChannel.getId() == 0 ? mService.HumlaSession().getPermissions()
                                         : mChannel.getPermissions();
        }
        return 0;
    }

    public void show() {
        mService.registerObserver(mPermissionsObserver);
        if (getPermissions() == 0) {
            // onMenuPrepare will be called once more once permissions have loaded.
            if (mService.isConnected()) {
                mService.HumlaSession().requestPermissions(mChannel.getId());
            }
        } else {
            mPrepareListener.onMenuPrepare(mMenu.getMenu(), getPermissions());
        }
        mMenu.show();
    }

    public void dismiss() {
        mMenu.dismiss();
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        mService.unregisterObserver(mPermissionsObserver);
    }

    /**
     * An enforcer of channel permissions, disabling menu items as appropriate.
     */
    public interface IOnMenuPrepareListener {
        void onMenuPrepare(Menu menu, int permissions);
    }
}
