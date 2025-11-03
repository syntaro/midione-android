package jp.gr.java_conf.syntarou.midione.ui.userchoice;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.R;
import jp.gr.java_conf.syntarou.midione.databinding.ViewUserChoiceElementBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class UserChoiceAdapter extends ListAdapter<IUserChoiceElement, UserChoiceAdapter.UserchoiceViewHolder> {
    protected ArrayList<IUserChoiceElement> _list;
    public UserChoiceAdapter() {
        super(new DiffUtil.ItemCallback<IUserChoiceElement>() {
            /* for enable Drag & Drop, This logic fine */
            @Override
            public boolean areItemsTheSame(@NonNull IUserChoiceElement oldItem, @NonNull IUserChoiceElement newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull IUserChoiceElement oldItem, @NonNull IUserChoiceElement newItem) {
                return oldItem == newItem;
            }
        });
        _list = new ArrayList<>();
    }

    ItemTouchHelper helper = null;
    protected ItemTouchHelper prepareHelper() {
        if (helper != null) {
            return helper;
        }
        helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN /*|ItemTouchHelper.START | ItemTouchHelper.END*/, 0
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                swap(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                //askAndRemove(viewHolder.getAdapterPosition());
            }
            /*

            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder){
                super.clearView(recyclerView, viewHolder);
                //alpha?
            }
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                //alpha?
            }*/
        });

        return helper;
    }


    public void setData(@NonNull ArrayList<IUserChoiceElement> list) {
        _list = list;
        submitList(new ArrayList<>(list));
    }

    public void getData(@NonNull ArrayList<IUserChoiceElement> list) {
        list.clear();;
        for (int i = 0; i < getItemCount(); ++ i) {
            list.add(getItem(i));
        }
    }

    public void swap(int from, int to) {
        _list.add(to, _list.remove(from));
        submitList(new ArrayList<>(_list));
    }

    public static class UserchoiceViewHolder extends RecyclerView.ViewHolder {
        ViewUserChoiceElementBinding _viewBinding;

        public UserchoiceViewHolder(ViewUserChoiceElementBinding binding) {
            super(binding.getRoot());
            _viewBinding = binding;
        }

        public ViewUserChoiceElementBinding getBinding() {
            return _viewBinding;
        }
    }

    @Override
    public UserchoiceViewHolder onCreateViewHolder(@NotNull ViewGroup var1, int viewType) {
        ViewUserChoiceElementBinding _willBind = ViewUserChoiceElementBinding.inflate(LayoutInflater.from(var1.getContext()));
        return new UserchoiceViewHolder(_willBind);
    }

    /*
        public long getItemId(int position) {
            return position;
        }
    */

    public void setChoicedStyle(UserchoiceViewHolder holder2, int position, boolean focus, boolean choiced) {
        View target = holder2.itemView;
        if (focus) {
            target.setBackgroundColor(Color.WHITE);
        } else {
            if (choiced) {
                target.setBackgroundColor(Color.YELLOW);
            } else {
                target.setBackgroundColor(Color.LTGRAY);
            }
        }
        target.setFocusable(true);
        target.setFocusableInTouchMode(true);
        target.postInvalidate();

        /*
        binding.textViewItem2.setTextColor(Color.LTGRAY);
        binding.textViewItem.setTextColor(Color.BLACK);
        if (choiced) {
            binding.textViewItem.setTextColor(0xFFB30037);
        }
        else {
            binding.textViewItem.setTextColor(Color.BLACK);
        }*/

    }

    @Override
    public int getItemViewType(int position) {
        IUserChoiceElement item = getItem(position);
        if (item == null) {
            return 1;
        }

        String subLabel1 = item.getSubLabelText();
        int subLabel2 = item.getSubLabel();
        if (subLabel1 == null && subLabel2 == 0) {
            return 2;
        }
        return 1;
    }

    IUserChoiceElement _last = null;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull UserchoiceViewHolder holder, int position) {
        try {
            //Log.e(AppConstant.TAG, "onBindViewHolder " + position);
            ViewUserChoiceElementBinding bindingView = holder._viewBinding;

            try {
                IUserChoiceElement elem = getItem(position);

                TextView t1 = bindingView.textViewItem;
                TextView t2 = bindingView.textViewItem2;

                if (true) {
                    View root = bindingView.getRoot();
                    ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    root.setLayoutParams(param);
                }

                if (elem == null) {
                    t1.setText(R.string.dummy_text);
                    t2.setText(R.string.dummy_text);
                    t2.setVisibility(TextView.INVISIBLE);
                    ViewGroup.LayoutParams param = t2.getLayoutParams();
                    param.height = 0;
                    t2.setLayoutParams(param);
                    t1.setTextColor(Color.BLACK);
                    setChoicedStyle(holder, position, false, false);
                    return;
                }
                View frame = bindingView.getRoot();
                frame.setOnTouchListener((view, event) -> {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_UP) {
                        // should do in onUserChoice setUserChoiceResult(elem);
                        setChoicedStyle(holder, position, false, true);
                        if (_onUserChoiceListener != null) {
                            _onUserChoiceListener.onUserChoice(this, elem);
                        }
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        setChoicedStyle(holder, position, true, false);
                        //if (helper != null) {
                        //    helper.startDrag(holder);
                        //}
                    } else if (action == MotionEvent.ACTION_CANCEL) {
                        setChoicedStyle(holder, position, false, false);
                    }
                    return true;
                });
                int label1 = elem.getNameRes();
                String label2 = elem.getNameText();
                int subLabel1 = elem.getSubLabel();
                String subLabel2 = elem.getSubLabelText();

                boolean selected = false;
                if (elem == null) {
                    //nullは選択不可能
                    selected = false;
                } else {
                    selected = isSelected(elem);
                }

                setChoicedStyle(holder, position, false, selected);
                if (label2 != null) {
                    t1.setText(label2);
                } else if (label1 != 0) {
                    t1.setText(label1);
                } else {
                    t1.setText("");
                }
                subLabel2 = elem.getSubLabelText();

                t1.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_START);
                boolean hasHeight = true;
                if (subLabel2 != null) {
                    t2.setText(subLabel2);
                } else if (subLabel1 != 0) {
                    t2.setText(subLabel1);
                } else {
                    hasHeight = false;
                    t2.setText("");
                }
                t2.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_END);

                if (hasHeight) {
                    ViewGroup.LayoutParams param = t2.getLayoutParams();
                    param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    t2.setLayoutParams(param);
                } else {
                    ViewGroup.LayoutParams param = t2.getLayoutParams();
                    param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    param.height = 1;
                    t2.setLayoutParams(param);
                }
            } catch (Throwable e) { // just for debug
                Log.e(AppConstant.MidiOneTag, e.getMessage(), e);
            }
        } catch (Throwable e) { // just for debug
            Log.e(AppConstant.MidiOneTag, e.getMessage(), e);
        }
    }

    public int indexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        for (int i = 0; i < _list.size(); ++i) {
            IUserChoiceElement v1 = _list.get(i);
            if (v1 == obj) {
                return i;
            }
            /*
            //Integerは上でいけるけど、Longは下の必要がある
            if (v1 != null && obj != null && v1.equals(obj)) {
                return i;
            }*/
        }
        return -1;
    }

    public interface IUserChoiceListener {
        void onUserChoice(UserChoiceAdapter adapter, IUserChoiceElement clicked);
    }

    protected IUserChoiceListener _onUserChoiceListener = null;

    public void setOnUserChoiceListener(IUserChoiceListener listener) {
        _onUserChoiceListener = listener;
    }

    public IUserChoiceElement[] listAllSelection() {
        if (_listSelection == null || _listSelection.size() == 0) {
            return null;
        }
        IUserChoiceElement[] result =new IUserChoiceElement[_listSelection.size()];
        _listSelection.toArray(result);
        return result;
    }
    public IUserChoiceElement getSelection() {
        if (_listSelection == null) {
            return null;
        }
        if (_listSelection.size() == 0) {
            return null;
        }
        return _listSelection.get(0);
    }

    ArrayList<IUserChoiceElement> _listSelection = null;

    public boolean isSelected(IUserChoiceElement e) {
        if (_listSelection == null) {
            return false;
        }
        for (IUserChoiceElement seek : _listSelection) {
            if (seek == e) {
                return true;
            }
        }
        return false;
    }

    public void setSelection(@Nullable IUserChoiceElement focus) {
        IUserChoiceElement oldFocus = getSelection();
        if (focus != oldFocus || _listSelection == null || _listSelection.size() != 1) {
            _listSelection = new ArrayList<>();
            _listSelection.add(focus);
            new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
                notifyItemRangeChanged(0, getItemCount());
            });
        }
    }

    public void setSelection(@Nullable Collection<IUserChoiceElement> focus) {
        if (focus == null || focus.size() == 0) {
            _listSelection = new ArrayList<>();
        }
        else {
            if (_listSelection == null) {
                _listSelection = new ArrayList<>();
            }
            ArrayList<IUserChoiceElement> newList = new ArrayList<>(focus);
            if (newList.size() == _listSelection.size()) {
                if (newList.containsAll(_listSelection)  && _listSelection.containsAll(newList)) {
                    return;
                }
            }

            _listSelection = newList;
            new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
                notifyItemRangeChanged(0, getItemCount());
            });
        }
    }

    public void setSelection(int newY) {
        List<IUserChoiceElement> list = getCurrentList();
        if (list.isEmpty()) {
            setSelection((IUserChoiceElement) null);
        }
        else {
            if (newY >= 0 && newY < list.size()) {
            }
            else {
                setSelection(list.get(newY));
            }
        }
    }
    public void prepareDragAndDrop(RecyclerView attached) {
        prepareHelper().attachToRecyclerView(attached);
    }
}
