/*
 * Copyright (C) 2016 SpiritCroc
 * Email: spiritcroc@gmail.com
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

package de.spiritcroc.akg_vertretungsplan.settings;

public class Keys {

    /**
     * Settings - credentials
     */

    public static final String USERNAME = "pref_username";

    public static final String PASSWORD = "pref_password";


    /**
     * Settings - general behaviour
     */

    public static final String AUTO_MARK_READ = "pref_auto_mark_read";

    public static final String AUTO_LOAD_ON_OPEN = "pref_auto_load_on_open";

    public static final String BACKGROUND_SERVICE = "pref_background_service";

    public static final String BACKGROUND_UPDATE_INTERVAL = "pref_background_update_interval";


    /**
     * Settings - notifications
     */

    public static final String NOTIFICATION_ENABLED = "pref_notification_enabled";

    public static final String NOTIFICATION_SOUND_ENABLED = "pref_notification_sound_enabled";

    public static final String NOTIFICATION_SOUND = "pref_notification_sound";

    public static final String LED_NOTIFICATION_ENABLED = "pref_led_notification_enabled";

    public static final String LED_NOTIFICATION_COLOR = "pref_led_notification_color";

    public static final String VIBRATE_NOTIFICATION_ENABLED = "pref_vibrate_notification_enabled";

    @Deprecated
    public static final String NOTIFICATION_HEADS_UP = "pref_notification_heads_up";

    public static final String NOTIFICATION_ONLY_IF_RELEVANT = "pref_notification_only_if_relevant";

    public static final String NOTIFICATION_GENERAL_NOT_RELEVANT =
            "pref_notification_general_not_relevant";

    public static final String NOTIFICATION_PREVIEW_RELEVANT_COLOR =
            "pref_notification_preview_relevant_color";

    public static final String NOTIFICATION_PREVIEW_RELEVANT_STYLE =
            "pref_notification_preview_relevant_style";

    public static final String NOTIFICATION_PREVIEW_GENERAL_COLOR =
            "pref_notification_preview_general_color";

    public static final String NOTIFICATION_PREVIEW_GENERAL_STYLE =
            "pref_notification_preview_general_style";

    public static final String NOTIFICATION_PREVIEW_IRRELEVANT_COLOR =
            "pref_notification_preview_irrelevant_color";

    public static final String NOTIFICATION_PREVIEW_IRRELEVANT_STYLE =
            "pref_notification_preview_irrelevant_style";

    public static final String NOTIFICATION_BUTTON_MARK_SEEN = "pref_notification_button_mark_seen";

    public static final String NOTIFICATION_BUTTON_MARK_READ = "pref_notification_button_mark_read";


    /**
     * Settings - tesla unread
     */

    public static final String TESLA_UNREAD_ENABLE = "pref_tesla_unread_enable";

    public static final String TESLA_UNREAD_USE_COMPLETE_COUNT =
            "pref_tesla_unread_use_complete_count";

    public static final String TESLA_UNREAD_INCLUDE_GENERAL_INFORMATION_COUNT =
            "pref_tesla_unread_include_general_information_count";


    /**
     * Settings - user interface
     */

    public static final String HIDE_ACTION_RELOAD = "pref_hide_action_reload";

    public static final String HIDE_TEXT_VIEW = "pref_hide_text_view";

    public static final String NO_CHANGE_SINCE_MAX_PRECISION = "pref_no_change_since_max_precision";

    public static final String SHOW_MARK_READ_AS_ACTION = "pref_show_mark_read_as_action";

    public static final String SHOW_FILTERED_PLAN_AS_ACTION = "pref_show_filtered_plan_as_action";

    public static final String FORMATTED_PLAN_REPLACE_TEACHER_SHORT_WITH_TEACHER_FULL =
            "pref_formatted_plan_replace_teacher_short_with_teacher_full";

    public static final String FORMATTED_PLAN_SHOW_TEACHER_FULL_AND_SHORT =
            "pref_formatted_plan_show_teacher_full_and_short";

    public static final String FORMATTED_PLAN_AUTO_SELECT_DAY =
            "pref_formatted_plan_auto_select_day";

    public static final String FORMATTED_PLAN_AUTO_SELECT_DAY_TIME =
            "pref_formatted_plan_auto_select_day_time";

    public static final String FORMATTED_PLAN_AUTO_SELECT_DAY_TIME_MINUTES =
            "pref_formatted_plan_auto_select_day_time_minutes";

    public static final String FILTER_GENERAL = "pref_filter_general";

    public static final String THEME = "pref_theme";

    public static final String DRAWER_ACTIVE_ITEM_TEXT_COLOR = "pref_drawer_active_item_text_color";

    public static final String WEB_PLAN_USE_CUSTOM_STYLE = "pref_web_plan_use_custom_style";

    public static final String WEB_PLAN_CUSTOM_STYLE = "pref_web_plan_custom_style";

    /**
     * Settings - user interface - formatted plan colors
     */

    public static final String HEADER_TEXT_TEXT_COLOR = "pref_header_text_text_color";

    public static final String HEADER_TEXT_BG_COLOR = "pref_header_text_background_color";

    public static final String HEADER_TEXT_TEXT_COLOR_HL = "pref_header_text_text_color_highlight";

    public static final String HEADER_TEXT_BG_COLOR_HL =
            "pref_header_text__background_color_highlight";// Don't fix typo to keep old setting

    public static final String CLASS_TEXT_TEXT_COLOR = "pref_class_text_text_color";

    public static final String CLASS_TEXT_BG_COLOR = "pref_class_text_background_color";

    public static final String NORMAL_TEXT_TEXT_COLOR = "pref_normal_text_text_color";

    public static final String NORMAL_TEXT_BG_COLOR = "pref_normal_text_background_color";

    public static final String NORMAL_TEXT_TEXT_COLOR_HL = "pref_normal_text_text_color_highlight";

    public static final String NORMAL_TEXT_BG_COLOR_HL =
            "pref_normal_text__background_color_highlight";// Don't fix typo to keep old setting

    public static final String RELEVANT_TEXT_TEXT_COLOR = "pref_relevant_text_text_color";

    public static final String RELEVANT_TEXT_BG_COLOR = "pref_relevant_text_background_color";

    public static final String RELEVANT_TEXT_TEXT_COLOR_HL =
            "pref_relevant_text_text_color_highlight";

    public static final String RELEVANT_TEXT_BG_COLOR_HL =
            "pref_relevant_text_background_color_highlight";

    /**
     * Settings - user interface - action bar
     */

    public static final String ACTION_BAR_NORMAL_BG_COLOR =
            "pref_action_bar_normal_background_color";

    public static final String ACTION_BAR_NORMAL_DARK_TEXT = "pref_action_bar_normal_dark_text";

    public static final String ACTION_BAR_FILTERED_BG_COLOR =
            "pref_action_bar_filtered_background_color";

    public static final String ACTION_BAR_FILTERED_DARK_TEXT = "pref_action_bar_filtered_dark_text";


    /**
     * Settings - lesson plan
     */

    public static final String LESSON_PLAN_AUT0_SELECT_DAY = "pref_lesson_plan_auto_select_day";

    public static final String LESSON_PLAN_AUTO_SELECT_DAY_TIME =
            "pref_lesson_plan_auto_select_day_time";

    public static final String LESSON_PLAN_AUTO_SELECT_DAY_TIME_MINUTES =
            "pref_lesson_plan_auto_select_day_time_minutes";

    public static final String TEACHER_SHORT_RELEVANCY_IGNORE_CASE =
            "pref_teacher_short_relevancy_ignore_case";

    public static final String LESSON_PLAN_COLOR_TIME = "pref_lesson_plan_color_time";

    public static final String LESSON_PLAN_COLOR_LESSON = "pref_lesson_plan_color_lesson";

    public static final String LESSON_PLAN_COLOR_FREE_TIME = "pref_lesson_plan_color_free_time";

    public static final String LESSON_PLAN_COLOR_ROOM = "pref_lesson_plan_color_room";

    public static final String LESSON_PLAN_COLOR_RELEVANT_INFORMATION =
            "pref_lesson_plan_color_relevant_information";

    public static final String LESSON_PLAN_COLOR_GENERAL_INFORMATION =
            "pref_lesson_plan_color_general_information";

    public static final String LESSON_PLAN_BG_COLOR_CURRENT_LESSON =
            "pref_lesson_plan_bg_color_current_lesson";


    /**
     * Settings - widget
     */

    public static final String WIDGET_OPENS_ACTIVITY = "pref_widget_opens_activity";

    public static final String WIDGET_TEXT_COLOR = "pref_widget_text_color";

    public static final String WIDGET_TEXT_COLOR_HL_RELEVANT =
            "pref_widget_text_color_highlight_relevant";

    public static final String WIDGET_TEXT_COLOR_HL_GENERAL =
            "pref_widget_text_color_highlight_general";

    public static final String WIDGET_TEXT_COLOR_HL = "pref_widget_text_color_highlight";

    public static final String WIDGET_LAST_UPDATE_SHOW_SECONDS =
            "pref_widget_last_update_show_seconds";

    public static final String WIDGET_LAST_UPDATE_RELEVANT = "pref_widget_last_update_relevant";

    public static final String WIDGET_LAST_UPDATE_GENERAL = "pref_widget_last_update_general";

    public static final String WIDGET_LAST_UPDATE_IRRELEVANT = "pref_widget_last_update_irrelevant";

    public static final String WIDGET_LAST_UPDATE_NONE = "pref_widget_last_update_none";


    /**
     * Settings - hidden debug
     */

    public static final String HIDDEN_DEBUG_ENABLED = "pref_hidden_debug_enabled";

    public static final String DEBUG_ALLOW_LOW_UPDATE_INTERVALS = "pref_allow_low_update_intervals";

    public static final String DEBUG_OPTION_SEND_DEBUG_MAIL = "pref_enable_option_send_debug_email";

    public static final String OWN_LOG = "pref_own_log";

    public static final String DEBUG_SECRET_CODE = "pref_debugging_enabled";

    public static final String DEBUG_SKIP_NETWORK_CHECK = "pref_skip_network_check";


    /**
     * App data
     */

    public static final String SEEN_DISCLAIMER = "pref_seen_disclaimer";

    public static final String SEEN_GREETER = "pref_seen_greeter";

    public static final String LAST_CHECKED = "pref_last_checked";

    public static final String LAST_UPDATE = "pref_last_update";

    public static final String HTML_LATEST = "pref_html_latest";

    public static final String CSS = "pref_css";

    public static final String LATEST_TITLE_1 = "pref_latest_title_1";

    public static final String LATEST_TITLE_2 = "pref_latest_title_2";

    public static final String LATEST_PLAN_1 = "pref_latest_plan_1";

    public static final String LATEST_PLAN_2 = "pref_latest_plan_2";

    public static final String CURRENT_TITLE_1 = "pref_current_title_1";

    public static final String CURRENT_TITLE_2 = "pref_current_title_2";

    public static final String CURRENT_PLAN_1 = "pref_current_plan_1";

    public static final String CURRENT_PLAN_2 = "pref_current_plan_2";

    public static final String LAST_OFFLINE = "pref_last_offline";

    public static final String UNSEEN_CHANGES = "pref_unseen_changes";

    public static final String ILLEGAL_PLAN = "pref_illegal_plan";

    public static final String RELOAD_ON_RESUME = "pref_reload_on_resume";

    public static final String FILTER_PLAN = "pref_filter_plan";

    public static final String PLAN = "pref_plan";

    public static final String CUSTOM_ADDRESS = "pref_custom_address";

    public static final String LAST_PLAN_TYPE = "last_plan_type";

    public static final String TEXT_VIEW_TEXT = "pref_text_view_text";

    public static final String LESSON_PLAN = "pref_lesson_plan";

    public static final String CLASS = "pref_class";

    public static final String LESSON_PLAN_SHOW_TIME = "pref_lesson_plan_show_time";

    public static final String LESSON_PLAN_SHOW_FULL_TIME = "pref_lesson_plan_show_full_time";

    public static final String LESSON_PLAN_SHOW_INFORMATION = "pref_lesson_plan_show_information";

    public static final String LESSON_PLAN_SHOW_SUBJECT_SHORT = "pref_lesson_plan_show_subject_short";

}
