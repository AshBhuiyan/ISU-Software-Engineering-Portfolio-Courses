package cycredit.io.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cycredit.io.R;
import cycredit.io.model.StoreItem;

public class SectionedStoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    
    public interface OnPurchaseClick {
        void onPurchase(StoreItem item);
    }
    
    private final List<Object> items = new ArrayList<>();
    private final OnPurchaseClick onPurchaseClick;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
    
    public SectionedStoreAdapter(List<StoreItem> data, OnPurchaseClick onPurchaseClick) {
        this.onPurchaseClick = onPurchaseClick;
        organizeByCategory(data);
    }
    
    private void organizeByCategory(List<StoreItem> data) {
        Map<String, List<StoreItem>> categoryMap = new HashMap<>();
        for (StoreItem item : data) {
            // Handle null/empty category - default to "Other"
            String category = item.getCategory();
            if (category == null || category.isEmpty() || "null".equalsIgnoreCase(category)) {
                category = "Other";
            }
            if (!categoryMap.containsKey(category)) {
                categoryMap.put(category, new ArrayList<>());
            }
            categoryMap.get(category).add(item);
        }
        
        // Add headers and items in order (progressively more expensive categories)
        String[] categoryOrder = {
            "Snacks",      // $1-10
            "Supplies",    // $5-30
            "Apparel",     // $20-80
            "Fun",         // $15-100
            "Tech",        // $30-200
            "Luxury",      // $100-500
            "Epic",        // $300+
            "Food",        // Legacy
            "Memorabilia", // Legacy
            "Technology",  // Legacy
            "General",     // Default
            "Other"        // Fallback for null
        };
        for (String category : categoryOrder) {
            if (categoryMap.containsKey(category)) {
                items.add(category); // Header
                items.addAll(categoryMap.get(category)); // Items
            }
        }
        // Add any remaining categories not in the predefined order
        for (Map.Entry<String, List<StoreItem>> entry : categoryMap.entrySet()) {
            if (!items.contains(entry.getKey())) {
                items.add(entry.getKey());
                items.addAll(entry.getValue());
            }
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_store, parent, false);
            return new ItemVH(v);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).text.setText((String) items.get(position));
        } else if (holder instanceof ItemVH) {
            StoreItem item = (StoreItem) items.get(position);
            ItemVH h = (ItemVH) holder;
            h.name.setText(item.getName());
            h.desc.setText(item.getDescription());
            h.price.setText(currency.format(item.getPrice()));
            h.buyBtn.setOnClickListener(v -> onPurchaseClick.onPurchase(item));
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView text;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            text = (TextView) itemView;
            text.setTextSize(20f);
            text.setPadding(32, 32, 16, 16);
            text.setTextColor(0xFF00C8FF); // Bright cyan - visible on dark bg
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setAllCaps(true);
            text.setLetterSpacing(0.1f);
        }
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
    
    public void updateData(List<StoreItem> newData) {
        items.clear();
        organizeByCategory(newData);
        notifyDataSetChanged();
    }
}

