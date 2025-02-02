package com.example.swob_deku.Models.GatewayServer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GatewayServerRecyclerAdapter extends RecyclerView.Adapter<GatewayServerRecyclerAdapter.ViewHolder> {

    private final AsyncListDiffer<GatewayServer> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    int recentsRenderLayout;
    Context context;

    public GatewayServerRecyclerAdapter(Context context, int recentsRenderLayout) {
        this.context = context;
        this.recentsRenderLayout = recentsRenderLayout;
    }

    public GatewayServerRecyclerAdapter() {}

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(this.recentsRenderLayout, parent, false);
        return new GatewayServerRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        GatewayServer gatewayServer = gatewayServerList.get(position);
        GatewayServer gatewayServer = mDiffer.getCurrentList().get(position);
        holder.url.setText(gatewayServer.getURL());
        holder.method.setText(gatewayServer.getMethod());
        holder.date.setText(Long.toString(gatewayServer.getDate()));
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<GatewayServer> list) {
        mDiffer.submitList(list);
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView url;
        TextView method;
        TextView date;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            this.url = itemView.findViewById(R.id.gateway_server_url);
            this.method = itemView.findViewById(R.id.gateway_server_method);
            this.date = itemView.findViewById(R.id.gateway_server_date);
        }
    }

    public static final DiffUtil.ItemCallback<GatewayServer> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<GatewayServer>() {
            @Override
            public boolean areItemsTheSame(@NonNull GatewayServer oldItem, @NonNull GatewayServer newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull GatewayServer oldItem, @NonNull GatewayServer newItem) {
                return oldItem.equals(newItem);
            }
        };
}
