package com.musesleep.musesleep.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.musesleep.musesleep.object.PastSessionObject;
import com.musesleep.musesleep.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.EventDataObjectHolder> {
    private ArrayList<PastSessionObject> mDataset;
    private MyClickListener myClickListener;

    public static class EventDataObjectHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageView;
        TextView upperText;
        TextView lowerText;
        MyClickListener myClickListener;

        public EventDataObjectHolder(View itemView, MyClickListener myClickListener) {
            super(itemView);

            upperText = (TextView) itemView.findViewById(R.id.upperTextView);
            lowerText = (TextView) itemView.findViewById(R.id.lowerTextView);
            imageView = (ImageView) itemView.findViewById(R.id.recyclerViewItemImageView);

            this.myClickListener = myClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public RecyclerViewAdapter(ArrayList<PastSessionObject> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public EventDataObjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_row, parent, false);

        EventDataObjectHolder dataObjectHolder = new EventDataObjectHolder(view, myClickListener);

        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(EventDataObjectHolder holder, int position) {
        holder.upperText.setText(mDataset.get(position).getUpperText());
        holder.lowerText.setText(mDataset.get(position).getLowerText());
        holder.imageView.setImageDrawable(mDataset.get(position).getDrawable());
    }

    public void addItem(PastSessionObject dataObj, int index) {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }
}
