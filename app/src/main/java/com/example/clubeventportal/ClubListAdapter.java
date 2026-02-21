package com.example.clubeventportal;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class ClubListAdapter extends RecyclerView.Adapter<ClubListAdapter.ViewHolder> {

    private final ArrayList<Club> list;
    private final Context context;

    public ClubListAdapter(Context context, ArrayList<Club> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_club_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Club club = list.get(position);

        // Only setting Name and Image now
        holder.tvName.setText(club.getClubName());

        // Load Profile Image
        if (club.getProfileImage() != null && !club.getProfileImage().isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(club.getProfileImage(), Base64.DEFAULT);
                Glide.with(context)
                        .load(imageBytes)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.imgProfile);
            } catch (Exception e) {
                holder.imgProfile.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            // Default placeholder if no image
            holder.imgProfile.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click -> Open Club Events
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ClubEventsActivity.class);
            intent.putExtra("clubId", club.getClubId());
            intent.putExtra("clubName", club.getClubName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvClubName);
            imgProfile = itemView.findViewById(R.id.imgClubProfile);
        }
    }
}