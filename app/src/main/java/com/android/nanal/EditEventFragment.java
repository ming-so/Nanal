package com.android.nanal;

//public class EditEventFragment extends Fragment implements EventHandler, OnColorSelectedListener {
//    private static final String TAG = "EditEventActivity";
//    private static final String COLOR_PICKER_DIALOG_TAG = "ColorPickerDialog";
//
//    private static final int REQUEST_CODE_COLOR_PICKER = 0;
//
//    private static final String BUNDLE_KEY_MODEL = "key_model";
//    private static final String BUNDLE_KEY_EDIT_STATE = "key_edit_state";
//    private static final String BUNDLE_KEY_EVENT = "key_event";
//    private static final String BUNDLE_KEY_READ_ONLY = "key_read_only";
//    private static final String BUNDLE_KEY_EDIT_ON_LAUNCH = "key_edit_on_launch";
//    private static final String BUNDLE_KEY_SHOW_COLOR_PALETTE = "show_color_palette";
//
//    private static final String BUNDLE_KEY_DATE_BUTTON_CLICKED = "date_button_clicked";
//
//    private static final boolean DEBUG = false;
//
//    private static final int TOKEN_EVENT = 1;
//    private static final int TOKEN_ATTENDEES = 1 << 1;
//    private static final int TOKEN_REMINDERS = 1 << 2;
//    private static final int TOKEN_CALENDARS = 1 << 3;
//    private static final int TOKEN_COLORS = 1 << 4;
//
//    private static final int TOKEN_ALL = TOKEN_EVENT | TOKEN_ATTENDEES | TOKEN_REMINDERS
//            | TOKEN_CALENDARS | TOKEN_COLORS;
//    private static final int TOKEN_UNITIALIZED = 1 << 31;
//    private final EventInfo mEvent;
//    private final Done mOnDone = new Done();
//    private final Intent mIntent;
//    public boolean mShowModifyDialogOnLaunch = false;
//    EditEventHelper mHelper;
//    CalendarEventModel mModel;
//    CalendarEventModel mOriginalModel;
//    CalendarEventModel mRestoreModel;
//    EditEventView mView;
//    QueryHandler mHandler;
//    int mModification = Utils.MODIFY_UNINITIALIZED;
//    /**
//     * A bitfield of TOKEN_* to keep track which query hasn't been completed
//     * yet. Once all queries have returned, the model can be applied to the
//     * view.
//     */
//    private int mOutstandingQueries = TOKEN_UNITIALIZED;
//    private AlertDialog mModifyDialog;
//    private EventBundle mEventBundle;
//    private ArrayList<ReminderEntry> mReminders;
//    private int mEventColor;
//    private boolean mEventColorInitialized = false;
//    private Uri mUri;
//    private long mBegin;
//    private long mEnd;
//    private long mCalendarId = -1;
//    private EventColorPickerDialog mColorPickerDialog;
//    private AppCompatActivity mContext;
//    private boolean mSaveOnDetach = true;
//    private boolean mIsReadOnly = false;
//    private boolean mShowColorPalette = false;
//    private boolean mTimeSelectedWasStartTime;
//    private boolean mDateSelectedWasStartDate;
//    private InputMethodManager mInputMethodManager;
//    private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            onActionBarItemSelected(v.getId());
//        }
//    };
//    private boolean mUseCustomActionBar;
//    private View.OnClickListener mOnColorPickerClicked = new View.OnClickListener() {
//
//        @Override
//        public void onClick(View v) {
//            int[] colors = mModel.getCalendarEventColors();
//            if (mColorPickerDialog == null) {
//                mColorPickerDialog = EventColorPickerDialog.newInstance(colors,
//                        mModel.getEventColor(), mModel.getCalendarColor(), mView.mIsMultipane);
//                mColorPickerDialog.setOnColorSelectedListener(EditEventFragment.this);
//            } else {
//                mColorPickerDialog.setCalendarColor(mModel.getCalendarColor());
//                mColorPickerDialog.setColors(colors, mModel.getEventColor());
//            }
//            final FragmentManager fragmentManager = getFragmentManager();
//            fragmentManager.executePendingTransactions();
//            if (!mColorPickerDialog.isAdded()) {
//                mColorPickerDialog.show(fragmentManager, COLOR_PICKER_DIALOG_TAG);
//            }
//        }
//    };
//}
