package forpdateam.ru.forpda.ui.fragments.theme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observer;

import forpdateam.ru.forpda.App;
import forpdateam.ru.forpda.R;
import forpdateam.ru.forpda.api.ApiUtils;
import forpdateam.ru.forpda.api.IBaseForumPost;
import forpdateam.ru.forpda.api.RequestFile;
import forpdateam.ru.forpda.api.events.models.NotificationEvent;
import forpdateam.ru.forpda.api.theme.editpost.models.AttachmentItem;
import forpdateam.ru.forpda.api.theme.editpost.models.EditPostForm;
import forpdateam.ru.forpda.api.theme.models.ThemePage;
import forpdateam.ru.forpda.apirx.RxApi;
import forpdateam.ru.forpda.client.ClientHelper;
import forpdateam.ru.forpda.common.FilePickHelper;
import forpdateam.ru.forpda.common.IntentHandler;
import forpdateam.ru.forpda.common.Preferences;
import forpdateam.ru.forpda.common.Utils;
import forpdateam.ru.forpda.common.webview.jsinterfaces.IPostFunctions;
import forpdateam.ru.forpda.entity.app.TabNotification;
import forpdateam.ru.forpda.ui.TabManager;
import forpdateam.ru.forpda.ui.fragments.TabFragment;
import forpdateam.ru.forpda.ui.fragments.favorites.FavoritesFragment;
import forpdateam.ru.forpda.ui.fragments.favorites.FavoritesHelper;
import forpdateam.ru.forpda.ui.fragments.history.HistoryFragment;
import forpdateam.ru.forpda.ui.fragments.theme.editpost.EditPostFragment;
import forpdateam.ru.forpda.ui.fragments.topics.TopicsFragment;
import forpdateam.ru.forpda.ui.views.FabOnScroll;
import forpdateam.ru.forpda.ui.views.messagepanel.MessagePanel;
import forpdateam.ru.forpda.ui.views.messagepanel.attachments.AttachmentsPopup;
import forpdateam.ru.forpda.ui.views.pagination.PaginationHelper;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;

/**
 * Created by radiationx on 20.10.16.
 */

public abstract class ThemeFragment extends TabFragment implements IPostFunctions {
    //Указывают на произведенное действие: переход назад, обновление, обычный переход по ссылке
    private final static String LOG_TAG = ThemeFragment.class.getSimpleName();
    protected final static int BACK_ACTION = 0, REFRESH_ACTION = 1, NORMAL_ACTION = 2;
    protected int loadAction = NORMAL_ACTION;
    protected MenuItem toggleMessagePanelItem;
    protected MenuItem refreshMenuItem;
    protected MenuItem copyLinkMenuItem;
    protected MenuItem searchOnPageMenuItem;
    protected MenuItem searchInThemeMenuItem;
    protected MenuItem searchPostsMenuItem;
    protected MenuItem deleteFavoritesMenuItem;
    protected MenuItem addFavoritesMenuItem;
    protected MenuItem openForumMenuItem;
    protected SwipeRefreshLayout refreshLayout;
    protected ThemePage currentPage;
    protected List<ThemePage> history = new ArrayList<>();
    //protected Subscriber<String> helperSubscriber = new Subscriber<>(this);
    private PaginationHelper paginationHelper;
    //Тег для вьюхи поиска. Чтобы создавались кнопки и т.д, только при вызове поиска, а не при каждом создании меню.
    protected int searchViewTag = 0;
    //protected final ColorFilter colorFilter = new PorterDuffColorFilter(Color.argb(80, 255, 255, 255), PorterDuff.Mode.DST_IN);
    protected MessagePanel messagePanel;
    protected AttachmentsPopup attachmentsPopup;
    protected String tab_url = "";
    protected SimpleTooltip tooltip;
    private View notificationView;
    private TextView notificationTitle;
    private ImageButton notificationButton;
    private Handler notificationHandler = new Handler(Looper.getMainLooper());
    private Runnable notifyRunnable = () -> notificationView.setVisibility(View.VISIBLE);

    private Observer themePreferenceObserver = (observable, o) -> {
        if (o == null) return;
        String key = (String) o;
        switch (key) {
            case Preferences.Theme.SHOW_AVATARS: {
                updateShowAvatarState(Preferences.Theme.isShowAvatars(getContext()));
                break;
            }
            case Preferences.Theme.CIRCLE_AVATARS: {
                updateTypeAvatarState(Preferences.Theme.isCircleAvatars(getContext()));
                break;
            }
            case Preferences.Main.WEBVIEW_FONT_SIZE: {
                setFontSize(Preferences.Main.getWebViewSize(getContext()));
            }
            case Preferences.Main.SCROLL_BUTTON_ENABLE: {
                if (Preferences.Main.isScrollButtonEnable(getContext())) {
                    fab.setVisibility(View.VISIBLE);
                } else {
                    fab.setVisibility(View.GONE);
                }
            }
        }
    };

    private Observer notification = (observable, o) -> {
        if (o == null) return;
        TabNotification event = (TabNotification) o;
        runInUiThread(() -> handleEvent(event));
    };

    private void handleEvent(TabNotification event) {
        Log.e("SUKAT", "handleEvent " + event.isWebSocket() + " : " + event.getSource() + " : " + event.getType());
        if (!event.isWebSocket())
            return;
        if (currentPage == null)
            return;
        Log.e("SUKAT", "handleEvent " + event.getEvent().getSourceId() + " : " + currentPage.getId());
        if (event.getEvent().getSourceId() != currentPage.getId())
            return;

        if (event.getSource() == NotificationEvent.Source.THEME) {
            switch (event.getType()) {
                case NEW:
                    onEventNew(event);
                    break;
                case READ:
                    onEventRead(event);
                    break;
                case MENTION:

                    break;
            }
        }

    }


    private void onEventNew(TabNotification event) {
        Log.d("SUKAT", "onEventNew");
        notificationHandler.postDelayed(notifyRunnable, 2000);

    }

    private void onEventRead(TabNotification event) {
        Log.d("SUKAT", "onEventRead");
        notificationHandler.removeCallbacks(notifyRunnable);
        notificationView.setVisibility(View.GONE);
    }


    protected abstract void addShowingView();

    protected abstract void findNext(boolean next);

    protected abstract void findText(String text);

    protected abstract void saveToHistory(ThemePage themePage);

    protected abstract void updateHistoryLast(ThemePage themePage);

    protected abstract void updateShowAvatarState(boolean isShow);

    protected abstract void updateTypeAvatarState(boolean isCircle);

    protected abstract void setFontSize(int size);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tab_url = getArguments().getString(ARG_TAB);
        }
    }

    @Override
    protected void initFabBehavior() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        FabOnScroll behavior = new FabOnScroll(fab.getContext(), null);
        params.setBehavior(behavior);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            params.setMargins(App.px16, App.px16, App.px16, App.px16);
        }
        fab.requestLayout();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        initFabBehavior();
        baseInflateFragment(inflater, R.layout.fragment_theme);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_list);
        messagePanel = new MessagePanel(getContext(), fragmentContainer, coordinatorLayout, false);
        paginationHelper = new PaginationHelper(getActivity());
        paginationHelper.addInToolbar(inflater, toolbarLayout, configuration.isFitSystemWindow());

        notificationView = inflater.inflate(R.layout.new_message_notification, null);
        notificationTitle = (TextView) notificationView.findViewById(R.id.title);
        notificationButton = (ImageButton) notificationView.findViewById(R.id.icon);
        fragmentContent.addView(notificationView);
        notificationView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        contentController.setMainRefresh(refreshLayout);

        addShowingView();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewsReady();
        setFontSize(Preferences.Main.getWebViewSize(getContext()));

        notificationButton.setColorFilter(App.getColorFromAttr(getContext(), R.attr.contrast_text_color), PorterDuff.Mode.SRC_ATOP);
        notificationTitle.setText("Новое сообщение");
        notificationView.setVisibility(View.GONE);
        notificationButton.setOnClickListener(v -> {
            notificationView.setVisibility(View.GONE);
        });
        notificationView.findViewById(R.id.new_message_card)
                .setOnClickListener(v -> {
                    tab_url = "https://4pda.ru/forum/index.php?showtopic=" + currentPage.getId() + "&view=getnewpost";
                    loadData(NORMAL_ACTION);
                    notificationView.setVisibility(View.GONE);
                });


        messagePanel.enableBehavior();
        messagePanel.addSendOnClickListener(v -> sendMessage());
        messagePanel.getSendButton().setOnLongClickListener(v -> {
            EditPostForm form = createEditPostForm();
            if (form != null) {
                TabManager.get().add(EditPostFragment.newInstance(createEditPostForm(), currentPage.getTitle()));
            }
            return true;
        });
        messagePanel.getFullButton().setVisibility(View.VISIBLE);
        messagePanel.getFullButton().setOnClickListener(v -> {
            EditPostForm form = createEditPostForm();
            if (form != null) {
                TabManager.get().add(EditPostFragment.newInstance(createEditPostForm(), currentPage.getTitle()));
            }
        });
        messagePanel.getHideButton().setVisibility(View.VISIBLE);
        messagePanel.getHideButton().setOnClickListener(v -> {
            hideMessagePanel();
        });
        attachmentsPopup = messagePanel.getAttachmentsPopup();
        attachmentsPopup.setAddOnClickListener(v -> tryPickFile());
        attachmentsPopup.setDeleteOnClickListener(v -> removeFiles());


        paginationHelper.setListener(new PaginationHelper.PaginationListener() {
            @Override
            public boolean onTabSelected(TabLayout.Tab tab) {
                return refreshLayout.isRefreshing();
            }

            @Override
            public void onSelectedPage(int pageNumber) {
                String url = "https://4pda.ru/forum/index.php?showtopic=";
                url = url.concat(Uri.parse(tab_url).getQueryParameter("showtopic"));
                if (pageNumber != 0) url = url.concat("&st=").concat(Integer.toString(pageNumber));
                tab_url = url;
                loadData(NORMAL_ACTION);
            }
        });

        fab.setSize(FloatingActionButton.SIZE_MINI);
        if (Preferences.Main.isScrollButtonEnable(getContext())) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
        }
        fab.setScaleX(0.0f);
        fab.setScaleY(0.0f);
        fab.setAlpha(0.0f);

        App.get().addPreferenceChangeObserver(themePreferenceObserver);
        refreshLayoutStyle(refreshLayout);
        refreshLayout.setOnRefreshListener(() -> {
            loadData(REFRESH_ACTION);
        });
        if (App.get().getPreferences().getBoolean("theme.tooltip.long_click_send", true)) {
            tooltip = new SimpleTooltip.Builder(getContext())
                    .anchorView(messagePanel.getSendButton())
                    .text(R.string.tooltip_full_form)
                    .gravity(Gravity.TOP)
                    .animated(false)
                    .modal(true)
                    .transparentOverlay(false)
                    .backgroundColor(Color.BLACK)
                    .textColor(Color.WHITE)
                    .padding((float) App.px16)
                    .build();
            tooltip.show();
            App.get().getPreferences().edit().putBoolean("theme.tooltip.long_click_send", false).apply();
        }

        if (Preferences.Main.isEditorDefaultHidden(getContext())) {
            hideMessagePanel();
        } else {
            showMessagePanel(false);
        }
        App.get().subscribeFavorites(notification);
    }

    @Override
    public void onResume() {
        super.onResume();
        messagePanel.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        messagePanel.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.get().removePreferenceChangeObserver(themePreferenceObserver);
        App.get().unSubscribeFavorites(notification);
        history.clear();
        messagePanel.onDestroy();
        if (paginationHelper != null)
            paginationHelper.destroy();
    }

    @Override
    public void hidePopupWindows() {
        super.hidePopupWindows();
        messagePanel.hidePopupWindows();
    }

    public abstract void scrollToAnchor(String anchor);

    @Override
    public boolean onBackPressed() {
        super.onBackPressed();
        if (tooltip != null && tooltip.isShowing()) {
            tooltip.dismiss();
            return true;
        }
        if (messagePanel.onBackPressed())
            return true;
        if (getMenu().findItem(R.id.action_search) != null && getMenu().findItem(R.id.action_search).isActionViewExpanded()) {
            toolbar.collapseActionView();
            return true;
        }
        if (App.get().getPreferences().getBoolean("theme.anchor_history", true)) {
            if (currentPage != null && currentPage.getAnchors().size() > 1) {
                currentPage.removeAnchor();
                scrollToAnchor(currentPage.getAnchor());
                return true;
            }
        }
        if (history.size() > 1) {
            setAction(BACK_ACTION);

            ThemePage removed = history.get(history.size() - 1);
            history.remove(history.size() - 1);
            currentPage = history.get(history.size() - 1);
            Log.e(LOG_TAG, "BACK PRESS REMOVE " + removed + " :" + currentPage);
            tab_url = currentPage.getUrl();
            updateView();
            return true;
        }
        if ((messagePanel.getMessage() != null && !messagePanel.getMessage().isEmpty()) || !messagePanel.getAttachments().isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.editpost_lose_changes)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        history.clear();
                        TabManager.get().remove(ThemeFragment.this);
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        }
        return false;
    }


    /*
    *
    * LOADING POST FUNCTIONS
    *
    * */

    public void setAction(int action) {
        this.loadAction = action;
    }

    protected abstract void updateHistoryLastHtml();

    public void loadData(int action) {
        setAction(action);
        if (action == NORMAL_ACTION || action == REFRESH_ACTION) {
            updateHistoryLastHtml();
        }
        loadData();
    }

    @Override
    public boolean loadData() {
        if (!super.loadData()) {
            return false;
        }
        setRefreshing(true);

        refreshToolbarMenuItems(false);
        boolean hatOpen = false;
        boolean pollOpen = false;
        if (currentPage != null) {
            hatOpen = currentPage.isHatOpen();
            pollOpen = currentPage.isPollOpen();
        }
        subscribe(RxApi.Theme().getTheme(tab_url, true, hatOpen, pollOpen), this::onLoadData, new ThemePage(), v -> loadData());
        return true;
    }

    protected void onLoadData(ThemePage newPage) throws Exception {
        setRefreshing(false);
        if (newPage == null || newPage.getId() == 0 || newPage.getUrl() == null) {
            if (currentPage != null)
                tab_url = currentPage.getUrl();
            return;
        }
        if (currentPage == null) {
            new Handler().postDelayed(() -> (appBarLayout).setExpanded(false, true), 225);
            /*if (!isTalkBackEnabled()) {
                new Handler().postDelayed(() -> (appBarLayout).setExpanded(false, true), 225);
            }*/
        }

        currentPage = newPage;

        if (loadAction == NORMAL_ACTION) {
            saveToHistory(currentPage);
        }
        if (loadAction == REFRESH_ACTION) {
            updateHistoryLast(currentPage);
        }
        updateFavorites(currentPage);
        updateMainHistory(currentPage);
        updateView();
    }

    protected void updateTitle() {
        setTitle(currentPage.getTitle());
        setTabTitle(String.format(getString(R.string.fragment_tab_title_theme), currentPage.getTitle()));
    }

    protected void updateSubTitle() {
        setSubtitle(String.valueOf(currentPage.getPagination().getCurrent()).concat("/").concat(String.valueOf(currentPage.getPagination().getAll())));
    }

    protected void updateFavorites(ThemePage themePage) {
        if (!ClientHelper.getAuthState()
                || themePage.getPagination().getCurrent() < themePage.getPagination().getAll())
            return;

        int topicId = themePage.getId();

        TabFragment parentTab = TabManager.get().get(getParentTag());
        if (parentTab == null) {
            parentTab = TabManager.get().getByClass(FavoritesFragment.class);
        }

        if (parentTab == null)
            return;

        if (parentTab instanceof FavoritesFragment) {
            ((FavoritesFragment) parentTab).markRead(topicId);
        } else if (parentTab instanceof TopicsFragment) {
            ((TopicsFragment) parentTab).markRead(topicId);
        }
    }

    protected void updateMainHistory(ThemePage themePage) {
        long time = System.currentTimeMillis();
        HistoryFragment.addToHistory(themePage.getId(), themePage.getUrl(), themePage.getTitle());
        Log.d("SUKA", "ADD TO HISTORY " + (System.currentTimeMillis() - time));
    }

    @CallSuper
    protected void updateView() {
        paginationHelper.updatePagination(currentPage.getPagination());
        tab_url = currentPage.getUrl();
        updateTitle();
        updateSubTitle();
        refreshToolbarMenuItems(true);
    }

    private void toggleMessagePanel() {
        if (messagePanel.getVisibility() == View.VISIBLE) {
            hideMessagePanel();
        } else {
            showMessagePanel(true);
        }
    }

    private void showMessagePanel(boolean showKeyboard) {
        if (messagePanel.getVisibility() != View.VISIBLE) {
            messagePanel.setVisibility(View.VISIBLE);
            if (showKeyboard) {
                messagePanel.show();
            }
            messagePanel.getHeightChangeListener().onChangedHeight(messagePanel.getLastHeight());
            toggleMessagePanelItem.setIcon(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_transcribe_close));
        }
        if (showKeyboard) {
            //messagePanel.getMessageField().setSelection(messagePanel.getMessageField().length());
            messagePanel.getMessageField().requestFocus();
            getMainActivity().showKeyboard(messagePanel.getMessageField());
        }
    }

    private void hideMessagePanel() {
        messagePanel.setVisibility(View.GONE);
        messagePanel.hidePopupWindows();
        hidePopupWindows();
        messagePanel.getHeightChangeListener().onChangedHeight(0);
        toggleMessagePanelItem.setIcon(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_create));
    }

    @Override
    protected void addBaseToolbarMenu(Menu menu) {
        super.addBaseToolbarMenu(menu);
        toggleMessagePanelItem = menu
                .add(R.string.reply)
                .setIcon(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_create))
                .setOnMenuItemClickListener(menuItem -> {
                    toggleMessagePanel();
                    return false;
                })
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        refreshMenuItem = menu.add(R.string.refresh)
                .setIcon(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_refresh))
                .setOnMenuItemClickListener(menuItem -> {
                    loadData(REFRESH_ACTION);
                    return false;
                });

        copyLinkMenuItem = menu.add(R.string.copy_link)
                .setOnMenuItemClickListener(menuItem -> {
                    String url = tab_url;
                    if (currentPage != null) {
                        url = "https://4pda.ru/forum/index.php?showtopic=" + currentPage.getId();
                    }
                    Utils.copyToClipBoard(url);
                    return false;
                });
        addSearchOnPageItem(menu);
        searchInThemeMenuItem = menu.add(R.string.search_in_theme)
                .setOnMenuItemClickListener(menuItem -> {
                    IntentHandler.handle("https://4pda.ru/forum/index.php?forums=" + currentPage.getForumId() + "&topics=" + currentPage.getId() + "&act=search&source=pst&result=posts");
                    return false;
                });
        searchPostsMenuItem = menu.add(R.string.search_my_posts)
                .setOnMenuItemClickListener(menuItem -> {
                    String url = "https://4pda.ru/forum/index.php?forums="
                            + currentPage.getForumId()
                            + "&topics="
                            + currentPage.getId()
                            + "&act=search&source=pst&result=posts&username=";

                    try {
                        url += URLEncoder.encode(App.get().getPreferences().getString("auth.user.nick", "null"), "windows-1251");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    IntentHandler.handle(url);
                    return false;
                });

        deleteFavoritesMenuItem = menu.add(R.string.delete_from_favorites)
                .setOnMenuItemClickListener(menuItem -> {
                    if (currentPage.getFavId() == 0) {
                        Toast.makeText(App.getContext(), R.string.fav_delete_error_id_not_found, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    FavoritesHelper.deleteWithDialog(getContext(), aBoolean -> {
                        Toast.makeText(App.getContext(), getString(aBoolean ? R.string.favorite_theme_deleted : R.string.error), Toast.LENGTH_SHORT).show();
                        currentPage.setInFavorite(!aBoolean);
                        refreshToolbarMenuItems(true);
                    }, currentPage.getFavId());
                    return false;
                });
        addFavoritesMenuItem = menu.add(R.string.add_to_favorites)
                .setOnMenuItemClickListener(menuItem -> {
                    FavoritesHelper.addWithDialog(getContext(), aBoolean -> {
                        Toast.makeText(App.getContext(), aBoolean ? getString(R.string.favorites_added) : getString(R.string.error), Toast.LENGTH_SHORT).show();
                        currentPage.setInFavorite(aBoolean);
                        refreshToolbarMenuItems(true);
                    }, currentPage.getId());
                    return false;
                });
        openForumMenuItem = menu.add(R.string.open_theme_forum)
                .setOnMenuItemClickListener(menuItem -> {
                    IntentHandler.handle("https://4pda.ru/forum/index.php?showforum=" + currentPage.getForumId());
                    return false;
                });

        refreshToolbarMenuItems(false);
    }

    @Override
    protected void refreshToolbarMenuItems(boolean enable) {
        super.refreshToolbarMenuItems(enable);
        if (enable) {
            boolean pageNotNull = !(currentPage == null || currentPage.getId() == 0 || currentPage.getUrl() == null);


            toggleMessagePanelItem.setEnabled(true);
            refreshMenuItem.setEnabled(true);
            copyLinkMenuItem.setEnabled(pageNotNull);
            searchInThemeMenuItem.setEnabled(pageNotNull);
            searchPostsMenuItem.setEnabled(pageNotNull);
            searchOnPageMenuItem.setEnabled(pageNotNull);
            deleteFavoritesMenuItem.setEnabled(pageNotNull);
            addFavoritesMenuItem.setEnabled(pageNotNull);
            if (pageNotNull) {
                if (currentPage.isInFavorite()) {
                    deleteFavoritesMenuItem.setVisible(true);
                    addFavoritesMenuItem.setVisible(false);
                } else {
                    deleteFavoritesMenuItem.setVisible(false);
                    addFavoritesMenuItem.setVisible(true);
                }
            }
            openForumMenuItem.setEnabled(pageNotNull);
        } else {
            toggleMessagePanelItem.setEnabled(false);
            refreshMenuItem.setEnabled(true);
            copyLinkMenuItem.setEnabled(false);
            searchInThemeMenuItem.setEnabled(false);
            searchPostsMenuItem.setEnabled(false);
            searchOnPageMenuItem.setEnabled(false);
            deleteFavoritesMenuItem.setEnabled(false);
            addFavoritesMenuItem.setEnabled(false);
            deleteFavoritesMenuItem.setVisible(false);
            addFavoritesMenuItem.setVisible(false);
            openForumMenuItem.setEnabled(false);
        }
        if (!ClientHelper.getAuthState()) {
            toggleMessagePanelItem.setVisible(false);
            deleteFavoritesMenuItem.setVisible(false);
            addFavoritesMenuItem.setVisible(false);
            searchPostsMenuItem.setEnabled(false);
            hideMessagePanel();
        }
    }

    private void addSearchOnPageItem(Menu menu) {
        toolbar.inflateMenu(R.menu.theme_search_menu);
        searchOnPageMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(searchOnPageMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                toggleMessagePanelItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                toggleMessagePanelItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                return true;
            }
        });
        SearchView searchView = (SearchView) searchOnPageMenuItem.getActionView();
        searchView.setTag(searchViewTag);

        searchView.setOnSearchClickListener(v -> {
            if (searchView.getTag().equals(searchViewTag)) {
                ImageView searchClose = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
                if (searchClose != null)
                    ((ViewGroup) searchClose.getParent()).removeView(searchClose);

                ViewGroup.LayoutParams navButtonsParams = new ViewGroup.LayoutParams(App.px48, App.px48);
                TypedValue outValue = new TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.actionBarItemBackground, outValue, true);

                AppCompatImageButton btnNext = new AppCompatImageButton(searchView.getContext());
                btnNext.setImageDrawable(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_search_next));
                btnNext.setBackgroundResource(outValue.resourceId);

                AppCompatImageButton btnPrev = new AppCompatImageButton(searchView.getContext());
                btnPrev.setImageDrawable(App.getVecDrawable(getContext(), R.drawable.ic_toolbar_search_prev));
                btnPrev.setBackgroundResource(outValue.resourceId);

                ((LinearLayout) searchView.getChildAt(0)).addView(btnPrev, navButtonsParams);
                ((LinearLayout) searchView.getChildAt(0)).addView(btnNext, navButtonsParams);

                btnNext.setOnClickListener(v1 -> findNext(true));
                btnPrev.setOnClickListener(v1 -> findNext(false));
                searchViewTag++;
            }
        });

        SearchManager searchManager = (SearchManager) getMainActivity().getSystemService(Context.SEARCH_SERVICE);
        if (null != searchManager) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getMainActivity().getComponentName()));
        }

        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                findText(newText);
                return false;
            }
        });
    }


    /*
    *
    * EDIT POST FUNCTIONS
    *
    * */

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public AttachmentsPopup getAttachmentsPopup() {
        return attachmentsPopup;
    }

    private EditPostForm createEditPostForm() {
        EditPostForm form = new EditPostForm();
        if (currentPage == null) {
            return null;
        }
        form.setForumId(currentPage.getForumId());
        form.setTopicId(currentPage.getId());
        form.setSt(currentPage.getPagination().getCurrent() * currentPage.getPagination().getPerPage());
        form.setMessage(messagePanel.getMessage());
        List<AttachmentItem> attachments = messagePanel.getAttachments();
        for (AttachmentItem item : attachments) {
            form.addAttachment(item);
        }
        return form;
    }

    public void onSendPostCompleted(ThemePage themePage) throws Exception {
        messagePanel.clearAttachments();
        messagePanel.clearMessage();
        onLoadData(themePage);
    }

    public void onEditPostCompleted(ThemePage themePage) throws Exception {
        onLoadData(themePage);
    }

    private void sendMessage() {
        hidePopupWindows();
        EditPostForm form = createEditPostForm();
        if (form != null) {
            messagePanel.setProgressState(true);
            subscribe(RxApi.EditPost().sendPost(form), s -> {
                messagePanel.setProgressState(false);
                if (s != currentPage) {
                    onLoadData(s);
                    messagePanel.clearAttachments();
                    messagePanel.clearMessage();
                    if (Preferences.Main.isEditorDefaultHidden(getContext())) {
                        hideMessagePanel();
                    }
                }
            }, currentPage);
        }

    }

    public void tryPickFile() {
        App.get().checkStoragePermission(() -> startActivityForResult(FilePickHelper.pickFile(false), REQUEST_PICK_FILE), App.getActivity());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            uploadFiles(FilePickHelper.onActivityResult(getContext(), data));
        }
    }

    public void uploadFiles(List<RequestFile> files) {
        List<AttachmentItem> pending = attachmentsPopup.preUploadFiles(files);
        subscribe(RxApi.EditPost().uploadFiles(0, files, pending), items -> attachmentsPopup.onUploadFiles(items), new ArrayList<>(), null);
    }

    public void removeFiles() {
        attachmentsPopup.preDeleteFiles();
        List<AttachmentItem> selectedFiles = attachmentsPopup.getSelected();
        subscribe(RxApi.EditPost().deleteFiles(0, selectedFiles), item -> attachmentsPopup.onDeleteFiles(selectedFiles), selectedFiles, null);
    }



    /*
    *
    * Post functions
    *
    * */

    public IBaseForumPost getPostById(int postId) {
        for (IBaseForumPost post : currentPage.getPosts())
            if (post.getId() == postId)
                return post;
        return null;
    }

    public void firstPage() {
        paginationHelper.firstPage();
    }

    public void prevPage() {
        paginationHelper.prevPage();
    }


    public void nextPage() {
        paginationHelper.nextPage();
    }

    public void lastPage() {
        paginationHelper.lastPage();
    }

    public void selectPage() {
        paginationHelper.selectPageDialog();
    }

    public void showUserMenu(final String postId) {
        showUserMenu(getPostById(Integer.parseInt(postId)));
    }

    public void showReputationMenu(final String postId) {
        showReputationMenu(getPostById(Integer.parseInt(postId)));
    }

    public void showPostMenu(final String postId) {
        showPostMenu(getPostById(Integer.parseInt(postId)));
    }

    public void reportPost(final String postId) {
        reportPost(getPostById(Integer.parseInt(postId)));
    }

    public void reply(final String postId) {
        reply(getPostById(Integer.parseInt(postId)));
    }

    @Override
    public void reply(IBaseForumPost post) {
        if (getContext() == null || post == null) {
            return;
        }
        String insert = String.format(Locale.getDefault(), "[snapback]%s[/snapback] [b]%s,[/b] \n", post.getId(), post.getNick());
        messagePanel.insertText(insert);
        showMessagePanel(true);
    }

    public void quotePost(final String text, final String postId) {
        quotePost(text, getPostById(Integer.parseInt(postId)));
    }

    @Override
    public void quotePost(String text, IBaseForumPost post) {
        if (getContext() == null) {
            return;
        }
        String date = Utils.getForumDateTime(Utils.parseForumDateTime(post.getDate()));
        String insert = String.format(Locale.getDefault(), "[quote name=\"%s\" date=\"%s\" post=%S]%s[/quote]\n", ApiUtils.escapeQuotes(post.getNick()), date, post.getId(), text);
        messagePanel.insertText(insert);
        showMessagePanel(true);
    }


    public void deletePost(final String postId) {
        deletePost(getPostById(Integer.parseInt(postId)));
    }

    public void editPost(final String postId) {
        editPost(getPostById(Integer.parseInt(postId)));
    }

    @Override
    public void editPost(IBaseForumPost post) {
        if (getContext() == null) {
            return;
        }
        TabManager.get().add(EditPostFragment.newInstance(post.getId(), currentPage.getId(), currentPage.getForumId(), currentPage.getSt(), currentPage.getTitle()));
    }

    public void votePost(final String postId, final boolean type) {
        votePost(getPostById(Integer.parseInt(postId)), type);
    }

    public void setHistoryBody(final String index, final String body) {
        setHistoryBody(Integer.parseInt(index), body);
    }

    public void setHistoryBody(int index, String body) {
        history.get(index).setHtml(body);
    }

    public void copySelectedText(final String text) {
        Utils.copyToClipBoard(text);
    }

    public void toast(final String text) {
        if (getContext() == null)
            return;
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }

    public void log(final String text) {
        int maxLogSize = 1000;
        for (int i = 0; i <= text.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = (i + 1) * maxLogSize;
            end = end > text.length() ? text.length() : end;
            Log.v(LOG_TAG, text.substring(start, end));
        }
    }

    public void showPollResults() {
        tab_url = tab_url.replaceFirst("#[^&]*", "").replace("&mode=show", "").replace("&poll_open=true", "").concat("&mode=show&poll_open=true");
        loadData(NORMAL_ACTION);
    }

    public void showPoll() {
        tab_url = tab_url.replaceFirst("#[^&]*", "").replace("&mode=show", "").replace("&poll_open=true", "").concat("&poll_open=true");
        loadData(NORMAL_ACTION);
    }

    @Override
    public void showUserMenu(IBaseForumPost post) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.showUserMenu(getContext(), this, post);
    }

    @Override
    public void showReputationMenu(IBaseForumPost post) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.showReputationMenu(getContext(), this, post);
    }

    @Override
    public void showPostMenu(IBaseForumPost post) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.showPostMenu(getContext(), this, post);
    }

    @Override
    public void reportPost(IBaseForumPost post) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.tryReportPost(getContext(), post);
    }

    //Удаление сообщения
    @Override
    public void deletePost(IBaseForumPost post) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.deletePost(getContext(), post, aBoolean -> {
            if (aBoolean)
                deletePostUi(post);

        });
    }

    public abstract void deletePostUi(IBaseForumPost post);

    //Изменение репутации сообщения
    @Override
    public void votePost(IBaseForumPost post, boolean type) {
        if (getContext() == null || post == null)
            return;
        new AlertDialog.Builder(getContext())
                .setMessage(String.format(getString(R.string.change_post_reputation_Type_Nick), getString(type ? R.string.increase : R.string.decrease), post.getNick()))

                .setPositiveButton(R.string.ok, (dialog, which) -> ThemeHelper.votePost(s -> toast(s.isEmpty() ? getString(R.string.unknown_error) : s), post.getId(), type))
                .setNegativeButton(R.string.cancel, null)
                .show();

    }

    //Изменение репутации пользователя
    @SuppressLint("InflateParams")
    @Override
    public void changeReputation(IBaseForumPost post, boolean type) {
        if (getContext() == null || post == null)
            return;
        ThemeDialogsHelper.changeReputation(getContext(), post, type);
    }

}
