package com.kiboi.fluffyqr;

import android.content.Context;
import android.graphics.Color;
import android.opengl.Visibility;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private ArrayList<Person> mPerson;
    private LayoutInflater mInflater;

    public MyAdapter(Context context, ArrayList<Person> personData)
    {
        this.mPerson = personData;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view  = mInflater.inflate(R.layout.item,viewGroup,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        Person myData = mPerson.get(i);
        myViewHolder.txtName.setText(myData.firstname+" "+myData.lastname);
        myViewHolder.txtDate.setText(myData.date);
    }

    @Override
    public int getItemCount() {
        return mPerson.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView txtName;
        private TextView txtDate;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtName);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }

}
