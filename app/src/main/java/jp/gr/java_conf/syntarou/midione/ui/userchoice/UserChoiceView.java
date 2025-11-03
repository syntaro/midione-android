package jp.gr.java_conf.syntarou.midione.ui.userchoice;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jp.gr.java_conf.syntarou.midione.R;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;

import java.util.ArrayList;

public class UserChoiceView extends RecyclerView {
    Button[] _positiveButton;

    public interface IPositiveChecker {
        boolean isPositiveResult(IUserChoiceElement elem);
    }

    IPositiveChecker _positiveChecker;

    public void setOnCheckPositive(Button[] positiveButton, IPositiveChecker checker) {
        _positiveChecker = checker;
        _positiveButton = positiveButton;
        if (_positiveButton != null) {
            for (Button button : _positiveButton) {
                if (button != null) {
                    button.setEnabled(false);
                }
            }
        }
    }

    void doPositiveCheck(IUserChoiceElement e) {
        if (_positiveChecker == null) {
            getAdapter().setSelection(e);
            return;
        }
        boolean enable = _positiveChecker.isPositiveResult(e);
        if (enable) {
            getAdapter().setSelection(e);
        }
        if (_positiveButton != null) {
            ColorStateList colorEnabled = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.color_button_blue));
            ColorStateList colorDisabled = ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.color_button_disabled));
            for (Button button : _positiveButton) {
                if (button != null) {
                    button.setEnabled(enable);
                    if (enable) {
                        button.setBackgroundTintList(colorEnabled);
                    } else {
                        button.setBackgroundTintList(colorDisabled);
                    }
                }
            }
        }
    }

    public UserChoiceView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public UserChoiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public UserChoiceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public IUserChoiceElement getUserChoiceResult() {
        return getAdapter().getSelection();
    }


    public void setData(ArrayList<IUserChoiceElement> list) {
        getAdapter().setData(list);
    }

    public void clicked(UserChoiceAdapter adapter, IUserChoiceElement elem) {
        doPositiveCheck(elem);
    }

    private boolean _overwriteMeasure = false;

    public void setNoscrollMyself(boolean overwriteMeasure) {
        _overwriteMeasure = overwriteMeasure;
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        int x = context.obtainStyledAttributes(attrs, R.styleable.UserChoiceView).getInt(R.styleable.UserChoiceView_useScroll, -1);
        if (x > 0) {
            _overwriteMeasure = true;
        } else {
            _overwriteMeasure = false;
        }

        setLayoutManager(new LinearLayoutManager(context));
        UserChoiceAdapter _myAdapter = new UserChoiceAdapter();
        setAdapter(_myAdapter);
        _myAdapter.setOnUserChoiceListener(this::clicked);
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (_overwriteMeasure) {
            int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
                    Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = getMeasuredHeight();
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public UserChoiceAdapter getAdapter() {
        return (UserChoiceAdapter) super.getAdapter();
    }

    public void repaintAdapter() {
        OneHelper.runOnUiThread(() -> {
            UserChoiceAdapter a = getAdapter();
            setAdapter(null);
            setAdapter(a);
            //a.notifyItemRangeChanged(0, a.getItemCount());
            //a.notifyDataSetChanged();
            postInvalidate();
        });
    }
}
