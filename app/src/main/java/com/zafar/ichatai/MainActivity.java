package com.zafar.ichatai;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.zafar.ichatai.data.local.AppDatabase;
import com.zafar.ichatai.data.local.dao.ChatDao;
import com.zafar.ichatai.data.local.dao.MessageDao;
import com.zafar.ichatai.data.local.entity.Chat;
import com.zafar.ichatai.data.local.entity.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversationAdapter.Listener {

    public static final String ACTION_CREDITS_CHANGED = "com.zafar.ichatai.CREDITS_CHANGED";
    private static final String STATE_CURRENT_CHAT_ID = "state_current_chat_id";
    private static final String STATE_CHAT_SAVED = "state_chat_saved";
    private static final String STATE_HAS_USER_MSG = "state_has_user_msg";
    private static final String STATE_DRAFT = "state_draft";
    private static final String STATE_MSG_TEXTS = "state_msg_texts";
    private static final String STATE_MSG_IS_USER = "state_msg_is_user";

    // Drawer + header
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnNewChat, btnCloseDrawer;
    private Chip chipCredits;
    private RecyclerView rvConversations;
    private TextInputEditText etSearch;

    // Chat UI
    private MessageAdapter adapter;
    private RecyclerView recyclerView;
    private TextInputEditText queryEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;

    // Data
    private AppDatabase db;
    private ChatDao chatDao;
    private MessageDao messageDao;
    private ConversationAdapter convAdapter;

    // Current chat tracking
    private long currentChatId = -1;   // -1 means not yet saved to DB
    private boolean chatSaved = false; // saved only after first AI reply

    // State
    private boolean hasAnyUserMessage = false; // enables New Chat after first user send (in this session)
    private boolean isSending = false;

    // Credits + ads
    private CreditsManager credits;
    private final Handler handler = new Handler();
    private boolean interstitialShown = false;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Basic flags
        outState.putLong(STATE_CURRENT_CHAT_ID, currentChatId);
        outState.putBoolean(STATE_CHAT_SAVED, chatSaved);
        outState.putBoolean(STATE_HAS_USER_MSG, hasAnyUserMessage);

        // Draft in the input
        String draft = (queryEditText.getText() != null) ? queryEditText.getText().toString() : "";
        outState.putString(STATE_DRAFT, draft);

        // Serialize visible messages (for unsaved chat sessions)
        ArrayList<String> texts = new ArrayList<>();
        ArrayList<Boolean> isUsers = new ArrayList<>();
        // add a getter in MessageAdapter (see step 3) or keep your own list
        for (Message m : adapter.getMessages()) {
            texts.add(m.getText());
            isUsers.add(m.isUser());
        }
        outState.putStringArrayList(STATE_MSG_TEXTS, texts);
        // convert to primitive boolean[]
        boolean[] isUserArray = new boolean[isUsers.size()];
        for (int i = 0; i < isUsers.size(); i++) isUserArray[i] = isUsers.get(i);
        outState.putBooleanArray(STATE_MSG_IS_USER, isUserArray);
    }

    // Receivers
    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!NetworkUtil.isConnected(context)) {
                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG).show();
            }
        }
    };

    private final BroadcastReceiver creditsReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateCreditsUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // DB + prefs
        db = AppDatabase.get(this);
        chatDao = db.chatDao();
        messageDao = db.messageDao();
        credits = new CreditsManager(this);

        // Views
        drawerLayout = findViewById(R.id.drawerLayout);
        View mainRoot = findViewById(R.id.main);

        btnMenu = findViewById(R.id.btnMenu);
        btnNewChat = findViewById(R.id.btnNewChat);
        chipCredits = findViewById(R.id.chipCredits);

        recyclerView = findViewById(R.id.recyclerView);
        queryEditText = findViewById(R.id.queryEditText);
        sendButton = findViewById(R.id.sendPromptButton);
        progressBar = findViewById(R.id.sendPromptProgressBar);

        btnCloseDrawer = findViewById(R.id.btnCloseActivity);
        rvConversations = findViewById(R.id.rvConversations);
        etSearch = findViewById(R.id.etSearch);
        View btnGetCredits = findViewById(R.id.btnGetCredits);
        View btnSettings = findViewById(R.id.btnSettings);

        // Insets for main root (avoid drawer insets to prevent "pinching")
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, Math.max(ime.bottom, sys.bottom));
            return insets;
        });

        // Drawer behavior: hide/show keyboard and refresh conversations on close
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(@NonNull View drawerView) { hideKeyboard(); }
            @Override public void onDrawerClosed(@NonNull View drawerView) {
                queryEditText.requestFocus();
                showKeyboard();
                if (etSearch.getText() != null && etSearch.getText().length() > 0) {
                    etSearch.setText("");
                    loadConversations();
                }
            }
        });

        // Header actions
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setNewChatEnabled(false);
        btnNewChat.setOnClickListener(v -> {
            if (!hasAnyUserMessage) {
                Toast.makeText(this, "Send your first message before starting a new chat.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isSending) {
                Toast.makeText(this, "Please wait for the current reply to finish.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (savedInstanceState == null) {
                // first launch only
                startUnsavedChat();
            } else {
                restoreFromState(savedInstanceState);
            }
        });

        // Drawer actions
        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        btnGetCredits.setOnClickListener(v -> startActivity(new Intent(this, CreditsActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Conversations list
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        convAdapter = new ConversationAdapter(this);
        rvConversations.setAdapter(convAdapter);
        etSearch.addTextChangedListener(new SimpleTextWatcher(s -> convAdapter.getFilter().filter(s)));

        // Back closes drawer first
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Messages list
        adapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Always start a fresh unsaved session
        startUnsavedChat();

        // Send logic (credits gated, single-flight while AI replies)
        sendButton.setOnClickListener(v -> handleSend());


        // Receivers
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(creditsReceiver, new IntentFilter(ACTION_CREDITS_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(creditsReceiver, new IntentFilter(ACTION_CREDITS_CHANGED));
        }

        // Keyboard
        queryEditText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Load saved conversations in drawer
        loadConversations();
        updateCreditsUI();

        // Interstitial (optional)
        AdHelper.warmUp(this);
        handler.postDelayed(() -> {
            if (!interstitialShown) {
                AdHelper.showInterstitial(this, shown -> interstitialShown = true);
            }
        }, 30_000);
    }

    private void restoreFromState(@NonNull Bundle state) {
        long restoredId = state.getLong(STATE_CURRENT_CHAT_ID, -1L);
        chatSaved = state.getBoolean(STATE_CHAT_SAVED, false);
        hasAnyUserMessage = state.getBoolean(STATE_HAS_USER_MSG, false);
        setNewChatEnabled(hasAnyUserMessage);

        String draft = state.getString(STATE_DRAFT, "");
        queryEditText.setText(draft);

        if (chatSaved && restoredId != -1L) {
            // for saved chats, reload from DB (keeps source of truth)
            loadMessagesFor(restoredId);  // already implemented
            return;
        }

        // otherwise restore the in-memory unsaved session
        ArrayList<String> texts = state.getStringArrayList(STATE_MSG_TEXTS);
        boolean[] isUserArray = state.getBooleanArray(STATE_MSG_IS_USER);

        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);
        if (texts != null && isUserArray != null) {
            for (int i = 0; i < texts.size() && i < isUserArray.length; i++) {
                adapter.addMessage(new Message(texts.get(i), isUserArray[i]));
            }
        } else {
            adapter.addMessage(new Message("New chat started. Ask me anything!", false));
            hasAnyUserMessage = false;
            setNewChatEnabled(false);
        }
        currentChatId = -1;
        scrollToBottom();
    }

    // Start a new unsaved chat session in UI (no DB writes yet)
    private void startUnsavedChat() {
        adapter = new MessageAdapter();
        recyclerView.setAdapter(adapter);
        adapter.addMessage(new Message("New chat started. Ask me anything!", false));
        scrollToBottom();
        currentChatId = -1;
        chatSaved = false;
        hasAnyUserMessage = false;
        setNewChatEnabled(false);
    }

    private void handleSend() {
        if (isSending) {
            Toast.makeText(this, "Please wait for the current reply to finish.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
            return;
        }
        String query = queryEditText.getText() != null ? queryEditText.getText().toString().trim() : "";
        if (query.isEmpty()) {
            Toast.makeText(this, "Please add your query", Toast.LENGTH_SHORT).show();
            return;
        }

        // Credits
        if (!credits.spend(1)) {
            new AlertDialog.Builder(this)
                    .setTitle("Out of credits")
                    .setMessage("Watch a short ad to earn 5 credits and continue.")
                    .setPositiveButton("Get credits", (d, w) -> startActivity(new Intent(this, CreditsActivity.class)))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        updateCreditsUI();
        sendBroadcast(new Intent(ACTION_CREDITS_CHANGED).setPackage(/* TODO: provide the application ID. For example: */ getPackageName()));

        // Add user's message to UI
        adapter.addMessage(new Message(query, true));
        hasAnyUserMessage = true;
        setNewChatEnabled(true);
        scrollToBottom();
        queryEditText.setText("");

        // Save user message immediately if chat already exists (later turns)
        if (chatSaved && currentChatId != -1) {
            saveUserMessage(currentChatId, query);
        }

        // Lock until AI responds
        setSending(true);
        adapter.addMessage(new Message("Thinking…", false));
        scrollToBottom();

        // AI call
        DeepSeekClient model = new DeepSeekClient();
        progressBar.setVisibility(View.VISIBLE);
        model.getResponse(query, new ResponseCallback() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    String content = (response == null || response.isEmpty()) ? "..." : response;
                    adapter.replaceLastAiMessage(content);
                    progressBar.setVisibility(View.GONE);
                    setSending(false);
                    scrollToBottom();
                    copyLastToClipboardIfLong(content);

                    // Persist chat only after first round completes (first turn)
                    if (!chatSaved) {
                        saveFirstRoundToDb(query, content);
                    } else {
                        // Subsequent turns: save AI message only (user already saved above)
                        saveAiMessage(currentChatId, content);
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    adapter.replaceLastAiMessage("Sorry, I ran into an error: " + throwable.getMessage());
                    progressBar.setVisibility(View.GONE);
                    setSending(false);
                    scrollToBottom();
                });
            }
        });
    }

    // Persist the first user+AI messages together and title the chat
    private void saveFirstRoundToDb(String userText, String aiText) {
        new Thread(() -> {
            try {
                long now = System.currentTimeMillis();
                db.runInTransaction(() -> {
                    // Title from first user message (trimmed to 40 chars)
                    String title = userText.trim();
                    if (title.length() > 40) title = title.substring(0, 40) + "…";

                    long newId = chatDao.insert(new Chat(title, now));
                    currentChatId = newId;

                    messageDao.insert(new ChatMessage(newId, true, userText, now));
                    messageDao.insert(new ChatMessage(newId, false, aiText, now));
                });
                chatSaved = true;
                runOnUiThread(this::loadConversationsClearingSearch);
            } catch (Exception ignored) {
                // If persisting fails, UI session remains; you can show a toast if desired.
            }
        }).start();
    }

    private void saveUserMessage(long chatId, String content) {
        new Thread(() ->
                messageDao.insert(new ChatMessage(chatId, true, content, System.currentTimeMillis()))
        ).start();
    }

    private void saveAiMessage(long chatId, String content) {
        new Thread(() -> messageDao.insert(new ChatMessage(chatId, false, content, System.currentTimeMillis()))).start();
    }

    private void loadMessagesFor(long chatId) {
        if (isSending) {
            Toast.makeText(this, "Please wait for the current reply to finish.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            List<ChatMessage> messages = messageDao.getByChat(chatId);
            runOnUiThread(() -> {
                adapter = new MessageAdapter();
                recyclerView.setAdapter(adapter);
                if (messages == null || messages.isEmpty()) {
                    adapter.addMessage(new Message("New chat started. Ask me anything!", false));
                } else {
                    for (ChatMessage m : messages) {
                        adapter.addMessage(new Message(m.content, m.isUser));
                    }
                }
                scrollToBottom();
                // This is a saved chat; allow new chat creation (user already sent before)
                hasAnyUserMessage = true;
                setNewChatEnabled(true);
                chatSaved = true;
                currentChatId = chatId;
            });
        }).start();
    }

    private void loadConversations() {
        new Thread(() -> {
            List<Chat> chats = chatDao.getAll();
            runOnUiThread(() -> convAdapter.setItems(chats));
        }).start();
        updateCreditsUI();
    }

    private void loadConversationsClearingSearch() {
        if (etSearch.getText() != null && etSearch.getText().length() > 0) {
            etSearch.setText("");
        }
        loadConversations();
    }

    private void updateCreditsUI() {
        chipCredits.setText(String.valueOf(credits.get()));
    }

    private void setNewChatEnabled(boolean enabled) {
        btnNewChat.setEnabled(enabled);
        btnNewChat.setAlpha(enabled ? 1f : 0.45f);
    }

    private void setSending(boolean sending) {
        isSending = sending;
        sendButton.setEnabled(!sending);
        queryEditText.setEnabled(!sending);
    }

    private void scrollToBottom() {
        recyclerView.post(() -> recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1)));
    }

    private void copyLastToClipboardIfLong(String text) {
        if (text != null && text.length() > 800) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Response", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Long response copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        queryEditText.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(queryEditText, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(networkReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(creditsReceiver); } catch (Exception ignored) {}
        handler.removeCallbacksAndMessages(null);
    }

    // Drawer callbacks
    @Override
    public void onChatClicked(@NonNull Chat chat) {
        drawerLayout.closeDrawer(GravityCompat.START);
        loadMessagesFor(chat.id);
    }

    @Override
    public void onRename(Chat chat) {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_rename_chat, null);
        TextInputEditText et = dialog.findViewById(R.id.etTitle);
        et.setText(chat.title);
        new AlertDialog.Builder(this)
                .setTitle("Rename chat")
                .setView(dialog)
                .setPositiveButton("Save", (d, w) -> {
                    String newTitle = et.getText() != null ? et.getText().toString().trim() : "";
                    if (!newTitle.isEmpty()) {
                        new Thread(() -> {
                            chatDao.updateTitle(chat.id, newTitle);
                            runOnUiThread(this::loadConversationsClearingSearch);
                        }).start();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(Chat chat) {
        if (isSending) {
            Toast.makeText(this, "Please wait for the current reply to finish.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete chat")
                .setMessage("This will delete the chat and its messages.")
                .setPositiveButton("Delete", (d, w) -> {
                    new Thread(() -> {
                        chatDao.deleteById(chat.id);
                        runOnUiThread(this::loadConversationsClearingSearch);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

// Connectivity util
class NetworkUtil {
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }
}