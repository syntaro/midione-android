package jp.gr.java_conf.syntarou.midione.ui.logview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import jp.gr.java_conf.syntarou.midione.databinding.ViewMessageLogElementBinding;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;

public class MessageLogAdapter extends RecyclerView.Adapter<MessageLogAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewMessageLogElementBinding binding = ViewMessageLogElementBinding.inflate(LayoutInflater.from(parent.getContext()), null, false);
        ViewHolder holder = new ViewHolder(binding);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewMessageLogElementBinding t = holder._binding;
        String e  = _list.get(position);
        t.textViewItem.setText(e);
    }


    public static String getText(OneMessage one) {
        if (one == null) {
            return "";
        }
        return one.toString();
    }

    @Override
    public int getItemCount() {
        return _list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ViewMessageLogElementBinding _binding;

        public ViewHolder(View view) {
            super(view);
            _binding = null;
        }

        public ViewHolder(ViewMessageLogElementBinding binding) {
            super(binding.getRoot());
            _binding = binding;
        }
    }

    MessageLogElementList _list;
    Context _context;

    public MessageLogAdapter(Context context, MessageLogElementList list) {
        _context = context;
        _list = list;
    }
}
