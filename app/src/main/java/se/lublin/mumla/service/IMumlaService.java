package se.lublin.mumla.service;

import com.morlunk.jumble.IJumbleService;

import java.util.List;

/**
 * Created by andrew on 28/02/17.
 */
public interface IMumlaService extends IJumbleService {
    void setOverlayShown(boolean showOverlay);

    boolean isOverlayShown();

    void clearChatNotifications();

    void markErrorShown();

    boolean isErrorShown();

    void onTalkKeyDown();

    void onTalkKeyUp();

    List<IChatMessage> getMessageLog();

    void clearMessageLog();

    void setSuppressNotifications(boolean suppressNotifications);
}
