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

package se.lublin.mumla.channel.comment;

import se.lublin.humla.IHumlaService;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.util.HumlaObserver;

/**
 * Created by andrew on 03/03/14.
 */
public class ChannelDescriptionFragment extends AbstractCommentFragment {

    @Override
    public void requestComment(final IHumlaService service) {
        if (!service.isConnected())
            return;
        service.registerObserver(new HumlaObserver() {
            @Override
            public void onChannelStateUpdated(IChannel channel) {
                if(channel.getId() == getChannelId() &&
                        channel.getDescription() != null) {
                    loadComment(channel.getDescription());
                    service.unregisterObserver(this);
                }
            }
        });
        service.HumlaSession().requestChannelDescription(getChannelId());
    }

    @Override
    public void editComment(IHumlaService service, String comment) {
        // TODO
    }

    private int getChannelId() {
        return getArguments().getInt("channel");
    }
}
