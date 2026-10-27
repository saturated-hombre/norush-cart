package com.project.norushcart.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnPlaceLabelUpdatedListener {
        void onPlaceLabelUpdated();
    }

    private double latitude;
    private double longitude;
    private List<ProductItem> items = new ArrayList<>();
    private OnPlaceLabelUpdatedListener listener;

    private TextView tvPlaceTitle;
    private TextView tvPlaceSubtitle;
    private MaterialButton btnLabelPlace;
    private RecyclerView rvPlaceItems;

    public static PlaceBottomSheetFragment newInstance(double lat, double lng, List<ProductItem> items) {
        PlaceBottomSheetFragment frag = new PlaceBottomSheetFragment();
        frag.latitude = lat;
        frag.longitude = lng;
        frag.items = items != null ? items : new ArrayList<>();
        return frag;
    }

    public void setOnPlaceLabelUpdatedListener(OnPlaceLabelUpdatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_place, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPlaceTitle = view.findViewById(R.id.tvPlaceTitle);
        tvPlaceSubtitle = view.findViewById(R.id.tvPlaceSubtitle);
        btnLabelPlace = view.findViewById(R.id.btnLabelPlace);
        rvPlaceItems = view.findViewById(R.id.rvPlaceItems);

        rvPlaceItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaceItems.setAdapter(new PlaceItemsAdapter(items));

        updateHeader();

        btnLabelPlace.setOnClickListener(v -> showLabelInputDialog());
    }

    private void updateHeader() {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        String savedLabel = db.getPlaceLabel(latitude, longitude);

        if (savedLabel != null && !savedLabel.isEmpty()) {
            tvPlaceTitle.setText(savedLabel);
            btnLabelPlace.setText("Edit Label");
        } else {
            String coords = String.format(Locale.getDefault(), "%.3f, %.3f", latitude, longitude);
            tvPlaceTitle.setText("Hotspot (" + coords + ")");
            btnLabelPlace.setText(R.string.map_sheet_label_this_place);
        }

        double totalAmount = 0.0;
        for (ProductItem item : items) {
            totalAmount += item.getPrice();
        }

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvPlaceSubtitle.setText(items.size() + " impulse buys quarantined · " + format.format(totalAmount));
    }

    private void showLabelInputDialog() {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        String currentLabel = db.getPlaceLabel(latitude, longitude);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.map_sheet_label_this_place);

        final EditText input = new EditText(requireContext());
        input.setHint(R.string.map_sheet_label_hint);
        if (currentLabel != null) {
            input.setText(currentLabel);
            input.setSelection(currentLabel.length());
        }
        builder.setView(input);

        builder.setPositiveButton(R.string.map_sheet_save_label, (dialog, which) -> {
            String newLabel = input.getText().toString().trim();
            if (!newLabel.isEmpty()) {
                db.upsertPlace(latitude, longitude, newLabel);
                updateHeader();
                if (listener != null) {
                    listener.onPlaceLabelUpdated();
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();
    }

    private class PlaceItemsAdapter extends RecyclerView.Adapter<PlaceItemsAdapter.VH> {
        private final List<ProductItem> list;

        PlaceItemsAdapter(List<ProductItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ProductItem it = list.get(position);
            holder.tvTitle.setText(it.getTitle());
            holder.tvPriceAndStatus.setText(it.getFormattedPrice() + " • " + it.getStatus());

            GlideHelper.loadProductImage(holder.itemView.getContext(), it.getImageUrl(), holder.ivThumb);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvTitle;
            TextView tvPriceAndStatus;

            VH(@NonNull View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.ivThumb);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvPriceAndStatus = itemView.findViewById(R.id.tvPriceAndStatus);
            }
        }
    }
}
