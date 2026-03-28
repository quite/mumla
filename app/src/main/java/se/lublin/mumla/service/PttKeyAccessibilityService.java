package se.lublin.mumla.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import se.lublin.mumla.Settings;
import se.lublin.mumla.service.ipc.TalkBroadcastReceiver;

public class PttKeyAccessibilityService extends AccessibilityService {
    private PowerManager.WakeLock pttWl;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        pttWl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mumla:ptt");
        pttWl.setReferenceCounted(false);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        int selectedKeyCode = Settings.getInstance(this).getPushToTalkKey();
        final boolean isPtt = selectedKeyCode > 0 && event.getKeyCode() == selectedKeyCode;
        if (!isPtt) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            // Keep CPU on while user holds the PTT
            if (pttWl != null && !pttWl.isHeld()) pttWl.acquire(60_000); // safety timeout 60s
            sendTalkBroadcast(TalkBroadcastReceiver.TALK_STATUS_ON);
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            sendTalkBroadcast(TalkBroadcastReceiver.TALK_STATUS_OFF);
            if (pttWl != null && pttWl.isHeld()) pttWl.release();
            return true;
        }
        return false;
    }

    private void sendTalkBroadcast(String status) {
        Intent intent = new Intent(TalkBroadcastReceiver.BROADCAST_TALK);
        intent.putExtra(TalkBroadcastReceiver.EXTRA_TALK_STATUS, status);
        sendBroadcast(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) { /* not used */ }

    @Override
    public void onInterrupt() { /* not used */ }
}
