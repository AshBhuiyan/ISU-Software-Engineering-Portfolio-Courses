package cycredit.io.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import cycredit.io.R;
import cycredit.io.model.StoreItem;

public class StoreItemAdapter extends RecyclerView.Adapter<StoreItemAdapter.ItemVH> {

    public interface OnPurchaseClick {
        void onPurchase(StoreItem item);
    }

    private final List<StoreItem> data;
    private final OnPurchaseClick onPurchaseClick;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

    public StoreItemAdapter(List<StoreItem> data, OnPurchaseClick onPurchaseClick) {
        this.data = data;
        this.onPurchaseClick = onPurchaseClick;
    }

    @NonNull @Override
    public ItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);
        return new ItemVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemVH h, int position) {
        StoreItem it = data.get(position);
        h.name.setText(it.getName());
        h.desc.setText(it.getDescription());
        h.price.setText(currency.format(it.getPrice()));
        h.buyBtn.setOnClickListener(v -> onPurchaseClick.onPurchase(it));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView name, desc, price;
        Button buyBtn;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemName);
            desc = itemView.findViewById(R.id.itemDesc);
            price = itemView.findViewById(R.id.itemPrice);
            buyBtn = itemView.findViewById(R.id.buyBtn);
        }
    }
}