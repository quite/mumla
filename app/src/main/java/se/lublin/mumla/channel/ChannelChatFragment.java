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
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.lublin.humla.IHumlaService;
import se.lublin.humla.IHumlaSession;
import se.lublin.humla.model.IChannel;
import se.lublin.humla.model.IMessage;
import se.lublin.humla.model.IUser;
import se.lublin.humla.model.User;
import se.lublin.humla.util.HumlaDisconnectedException;
import se.lublin.humla.util.HumlaObserver;
import se.lublin.humla.util.IHumlaObserver;
import se.lublin.mumla.R;
import se.lublin.mumla.service.IChatMessage;
import se.lublin.mumla.util.BitmapUtils;
import se.lublin.mumla.util.HumlaServiceFragment;
import se.lublin.mumla.util.MumbleImageGetter;

public class ChannelChatFragment extends HumlaServiceFragment implements ChatTargetProvider.OnChatTargetSelectedListener {
    private static final String TAG = ChannelChatFragment.class.getName();

    private static final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+)");

    private IHumlaObserver mServiceObserver = new HumlaObserver() {

        @Override
        public void onMessageLogged(IMessage message) {
            addChatMessage(new IChatMessage.TextMessage(message), true);
        }

        @Override
        public void onLogInfo(String message) {
            addChatMessage(new IChatMessage.InfoMessage(IChatMessage.InfoMessage.Type.INFO, message), true);
        }

        @Override
        public void onLogWarning(String message) {
            addChatMessage(new IChatMessage.InfoMessage(IChatMessage.InfoMessage.Type.WARNING, message), true);
        }

        @Override
        public void onLogError(String message) {
            addChatMessage(new IChatMessage.InfoMessage(IChatMessage.InfoMessage.Type.ERROR, message), true);
        }

        @Override
        public void onUserJoinedChannel(IUser user, IChannel newChannel, IChannel oldChannel) {
            IHumlaService service = getService();
            if (service.isConnected()) {
                IHumlaSession session = service.HumlaSession();
                if (user != null && session.getSessionUser() != null &&
                        user.equals(session.getSessionUser()) &&
                        mTargetProvider.getChatTarget() == null) {
                    // Update chat target when user changes channels without a target.
                    updateChatTargetText(null);
                }
            }
        }
    };

    private ListView mChatList;
    private ChannelChatAdapter mChatAdapter;
    private EditText mChatTextEdit;
    private ImageButton mSendButton;
    private ChatTargetProvider mTargetProvider;
    ActivityResultLauncher<String> imagePicker
            = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

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
            throw new ClassCastException(getParentFragment().toString() + " must implement ChatTargetProvider");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mTargetProvider.registerChatTargetListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTargetProvider.unregisterChatTargetListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        mChatList = (ListView) view.findViewById(R.id.chat_list);
        mChatTextEdit = (EditText) view.findViewById(R.id.chatTextEdit);

        ImageButton ImageSendButton = view.findViewById(R.id.chatImageSend);
        ImageSendButton.setOnClickListener(buttonView -> imagePicker.launch("image/*"));

        mSendButton = (ImageButton) view.findViewById(R.id.chatTextSend);
        mSendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendMessageFromEditor();
                } catch (HumlaDisconnectedException e) {
                    Log.d(TAG, "exception from sendMessage: " + e);
                }
            }
        });

        mChatTextEdit.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                try {
                    sendMessageFromEditor();
                } catch (HumlaDisconnectedException e) {
                    Log.d(TAG, "exception from sendMessage: " + e);
                }
                return true;
            }
        });

        mChatTextEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSendButton.setEnabled(mChatTextEdit.getText().length() > 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateChatTargetText(mTargetProvider.getChatTarget());
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear_chat) {
            clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds the passed text to the fragment chat body.
     *
     * @param message The message to add.
     * @param scroll  Whether to scroll to the bottom after adding the message.
     */
    public void addChatMessage(IChatMessage message, boolean scroll) {
        if (mChatAdapter == null) return;

        mChatAdapter.add(message);

        if (scroll) {
            mChatList.post(new Runnable() {

                @Override
                public void run() {
                    mChatList.smoothScrollToPosition(mChatAdapter.getCount() - 1);
                }
            });
        }
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        if (getService() == null || !getService().isConnected()) {
            return;
        }

        // We don't fail on errors when getting orientation
        boolean flipped = false;
        int rotationDeg = 0;
        try (InputStream imageStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (imageStream == null) {
                Log.w(TAG, "openInputStream(uri) failed for orientation");
            } else {
                ExifInterface exif = new ExifInterface(imageStream);
                flipped = exif.isFlipped();
                rotationDeg = exif.getRotationDegrees();
            }
        } catch (IOException e) {
            Log.w(TAG, "exception when getting orientation: " + e);
        }
        Log.d(TAG, "flipped:" + flipped + " rotationDeg:" + rotationDeg);

        InputStream imageStream;
        try {
            imageStream = requireContext().getContentResolver().openInputStream(uri);
            if (imageStream == null) {
                Log.w(TAG, "openInputStream(uri) failed");
                return;
            }
        } catch (IOException e) {
            Log.w(TAG, "exception when opening stream: " + e);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
        if (bitmap == null) {
            Log.w(TAG, "decode to bitmap failed");
            return;
        }

        ImageView image = new ImageView(requireContext());
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setMaxHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 3);

        if (flipped || rotationDeg > 0) {
            Matrix matrix = new Matrix();
            if (flipped) {
                // first flip horizontally, following {@link ExifInterface#getRotationDegrees()}
                matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
            }
            if (rotationDeg > 0) {
                matrix.postRotate(rotationDeg);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, false);
        }

        Bitmap resized = BitmapUtils.resizeKeepingAspect(bitmap, 600, 400);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setMessage(R.string.image_confirm_send)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    onImageConfirmed(resized);
                })
                .setNegativeButton(R.string.cancel,  (dialog, which) -> dialog.dismiss())
                .setView(image);
        builder.create().show();
    }

    private void onImageConfirmed( Bitmap resized){
        int maxSize = getService().HumlaSession().getServerSettings().getImageMessageLength();

        // Try to resize image until it fits
        int quality = 97;
        byte[] compressed;
        do {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                Log.w(TAG, "compress failed, quality==" + quality);
            } else {
                compressed = stream.toByteArray();
                // Account for the base64 overhead
                if (4 * (compressed.length / 3) + 4 < maxSize || maxSize == 0) {
                    break;
                } else {
                    Log.d(TAG, "compress(quality==" + quality + ") >= " + maxSize + " bytes");
                }
            }
            compressed = null;
            quality -= 10;
        } while (quality > 0);

        if (compressed == null) {
            Log.w(TAG, "all compress attempts failed");
            return;
        }

        String imageStr = Base64.encodeToString(compressed, Base64.NO_WRAP);
        String encoded = URLEncoder.encode(imageStr);
        sendMessage("<img src=\"data:image/jpeg;base64," + encoded + "\"/>");
    }

    /**
     * Sends the message currently in {@link se.lublin.mumla.channel.ChannelChatFragment#mChatTextEdit}
     * to the remote server. Clears the message box if the message was sent successfully.
     *
     * @throws HumlaDisconnectedException If the service is disconnected.
     */
    private void sendMessageFromEditor() throws HumlaDisconnectedException {
        if (mChatTextEdit.length() == 0) return;
        String message = mChatTextEdit.getText().toString();
        sendMessage(message);
        mChatTextEdit.setText("");
    }

    /**
     * Sends a message to the remote server.
     *
     * @param message The message to send.
     * @throws HumlaDisconnectedException If the service is disconnected.
     */
    private void sendMessage(String message) throws HumlaDisconnectedException {
        String formattedMessage = markupOutgoingMessage(message);
        ChatTargetProvider.ChatTarget target = mTargetProvider.getChatTarget();
        IMessage responseMessage = null;
        IHumlaSession session = getService().HumlaSession();
        if (target == null)
            responseMessage = session.sendChannelTextMessage(session.getSessionChannel().getId(), formattedMessage, false);
        else if (target.getUser() != null)
            responseMessage = session.sendUserTextMessage(target.getUser().getSession(), formattedMessage);
        else if (target.getChannel() != null)
            responseMessage = session.sendChannelTextMessage(target.getChannel().getId(), formattedMessage, false);
        addChatMessage(new IChatMessage.TextMessage(responseMessage), true);
    }

    /**
     * Adds HTML markup to the message, replacing links and newlines.
     *
     * @param message The message to markup.
     * @return HTML data.
     */
    private String markupOutgoingMessage(String message) {
        String formattedBody = message;
        Matcher matcher = LINK_PATTERN.matcher(formattedBody);
        formattedBody = matcher.replaceAll("<a href=\"$1\">$1</a>")
                .replaceAll("\n", "<br>");
        return formattedBody;
    }

    public void clear() {
        if (mChatAdapter != null) {
            mChatAdapter.clear();
        }
        getService().clearMessageLog();
    }

    /**
     * Updates hint displaying chat target.
     */
    public void updateChatTargetText(final ChatTargetProvider.ChatTarget target) {
        if (getService() == null || !getService().isConnected()) return;

        IHumlaSession session = getService().HumlaSession();
        String hint = null;
        if (target == null && session.getSessionChannel() != null) {
            hint = getString(R.string.messageToChannel, session.getSessionChannel().getName());
        } else if (target != null && target.getUser() != null) {
            hint = getString(R.string.messageToUser, target.getUser().getName());
        } else if (target != null && target.getChannel() != null) {
            hint = getString(R.string.messageToChannel, target.getChannel().getName());
        }
        mChatTextEdit.setHint(hint);
        mChatTextEdit.requestLayout(); // Needed to update bounds after hint change.
    }


    @Override
    public void onServiceBound(IHumlaService service) {
        mChatAdapter = new ChannelChatAdapter(getActivity(), service, getService().getMessageLog());
        mChatList.setAdapter(mChatAdapter);
        mChatList.post(new Runnable() {
            @Override
            public void run() {
                mChatList.setSelection(mChatAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public IHumlaObserver getServiceObserver() {
        return mServiceObserver;
    }

    @Override
    public void onChatTargetSelected(ChatTargetProvider.ChatTarget target) {
        updateChatTargetText(target);
    }

    private static class ChannelChatAdapter extends ArrayAdapter<IChatMessage> {
        private final MumbleImageGetter mImageGetter;
        private final IHumlaService mService;
        private final DateFormat mDateFormat;

        public ChannelChatAdapter(Context context, IHumlaService service, List<IChatMessage> messages) {
            super(context, 0, new ArrayList<>(messages));
            mService = service;
            mImageGetter = new MumbleImageGetter(context);
            mDateFormat = SimpleDateFormat.getTimeInstance();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.list_chat_item, parent, false);
            }

            final LinearLayout chatBox = (LinearLayout) v.findViewById(R.id.list_chat_item_box);
            final TextView targetText = (TextView) v.findViewById(R.id.list_chat_item_target);
            final TextView messageText = (TextView) v.findViewById(R.id.list_chat_item_text);
            TextView timeText = (TextView) v.findViewById(R.id.list_chat_item_time);

            IChatMessage message = getItem(position);
            message.accept(new IChatMessage.Visitor() {
                @Override
                public void visit(IChatMessage.TextMessage message) {
                    IMessage textMessage = message.getMessage();
                    String targetMessage = getContext().getString(R.string.unknown);
                    boolean selfAuthored;
                    try {
                        selfAuthored = textMessage.getActor() == mService.HumlaSession().getSessionId();
                    } catch (HumlaDisconnectedException e) {
                        selfAuthored = false;
                    }

                    if (textMessage.getTargetChannels() != null && !textMessage.getTargetChannels().isEmpty()) {
                        IChannel currentChannel = (IChannel) textMessage.getTargetChannels().get(0);
                        if (currentChannel != null && currentChannel.getName() != null) {
                            targetMessage = getContext().getString(R.string.chat_message_to, textMessage.getActorName(), currentChannel.getName());
                        }
                    } else if (textMessage.getTargetTrees() != null && !textMessage.getTargetTrees().isEmpty()) {
                        IChannel currentChannel = (IChannel) textMessage.getTargetTrees().get(0);
                        if (currentChannel != null && currentChannel.getName() != null) {
                            targetMessage = getContext().getString(R.string.chat_message_to, textMessage.getActorName(), currentChannel.getName());
                        }
                    } else if (textMessage.getTargetUsers() != null && !textMessage.getTargetUsers().isEmpty()) {
                        User user = textMessage.getTargetUsers().get(0);
                        if (user != null && user.getName() != null) {
                            targetMessage = getContext().getString(R.string.chat_message_to, textMessage.getActorName(), user.getName());
                        }
                    } else {
                        targetMessage = textMessage.getActorName();
                    }

                    int gravity = selfAuthored ? Gravity.RIGHT : Gravity.LEFT;
                    chatBox.setGravity(gravity);
                    messageText.setGravity(gravity);
                    targetText.setText(targetMessage);
                    targetText.setVisibility(View.VISIBLE);
                }

                @Override
                public void visit(IChatMessage.InfoMessage message) {
                    targetText.setVisibility(View.GONE);
                    chatBox.setGravity(Gravity.LEFT);
                    messageText.setGravity(Gravity.LEFT);
                }
            });
            timeText.setText(mDateFormat.format(new Date(message.getReceivedTime())));
            messageText.setText(Html.fromHtml(message.getBody(), mImageGetter, null));
            messageText.setMovementMethod(LinkMovementMethod.getInstance());

            return v;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false; // Makes links clickable.
        }
    }
}
