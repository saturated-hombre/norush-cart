package com.project.norushcart.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.R;
import com.project.norushcart.data.NearbyStore;

public class StoreBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_STORE = "arg_store";

    public static StoreBottomSheetFragment newInstance(NearbyStore store) {
        StoreBottomSheetFragment fragment = new StoreBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STORE, store);
        fragment.setArguments(args);
        return fragment;
    }

    private NearbyStore store;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            store = (NearbyStore) getArguments().getSerializable(ARG_STORE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_store, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (store == null) {
            dismiss();
            return;
        }

        TextView tvStoreName = view.findViewById(R.id.tvStoreName);
        TextView tvStoreAddress = view.findViewById(R.id.tvStoreAddress);
        TextView tvStoreDistance = view.findViewById(R.id.tvStoreDistance);
        TextView tvStoreOpenStatus = view.findViewById(R.id.tvStoreOpenStatus);
        MaterialButton btnCallStore = view.findViewById(R.id.btnCallStore);
        MaterialButton btnDirections = view.findViewById(R.id.btnDirections);

        tvStoreName.setText(store.getName());
        tvStoreAddress.setText(store.getAddress().isEmpty() ? "Address unavailable" : store.getAddress());
        tvStoreDistance.setText("📍 " + store.getFormattedDistance());
        tvStoreOpenStatus.setText(store.getOpenStatusText());

        // Call Action
        if (store.getPhoneNumber() != null && !store.getPhoneNumber().trim().isEmpty()) {
            btnCallStore.setEnabled(true);
            btnCallStore.setOnClickListener(v -> {
                try {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(store.getPhoneNumber())));
                    startActivity(dialIntent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open phone dialer.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnCallStore.setEnabled(false);
            btnCallStore.setText("No Phone Listed");
            btnCallStore.setAlpha(0.5f);
        }

        // Directions Action (Google Maps Intent)
        btnDirections.setOnClickListener(v -> {
            try {
                Uri navUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                        + store.getLatitude() + "," + store.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, navUri);
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    intent.setPackage(null);
                    startActivity(intent);
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to launch Google Maps.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
