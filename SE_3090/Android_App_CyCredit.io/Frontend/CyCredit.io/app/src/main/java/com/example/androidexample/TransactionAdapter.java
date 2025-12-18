package cycredit.io.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import cycredit.io.R;
import cycredit.io.model.Transaction;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TxVH> {

    private final List<Transaction> data;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter fmtIn = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final DateTimeFormatter fmtOut = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    public TransactionAdapter(List<Transaction> data) {
        this.data = data;
    }

    @NonNull @Override
    public TxVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TxVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TxVH h, int position) {
        Transaction t = data.get(position);
        h.merchant.setText(t.getMerchant());
        h.category.setText(t.getCategory());

        try {
            OffsetDateTime odt = OffsetDateTime.parse(t.getIsoTimestamp(), fmtIn);
            h.time.setText(fmtOut.format(odt));
        } catch (Exception e) {
            h.time.setText(t.getIsoTimestamp());
        }

        // Display amount with proper sign based on transaction type
        // Charges (PURCHASE, INTEREST, FEE) show as negative
        // Credits (PAYMENT, INCOME, REWARD) show as positive
        double displayAmount = t.isCharge() ? -t.getAmount() : t.getAmount();
        String amt = currency.format(displayAmount);
        h.amount.setText(amt);
        
        // Set color: red for charges, green for credits
        int color = t.isCharge() 
            ? h.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark, null)
            : h.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark, null);
        h.amount.setTextColor(color);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class TxVH extends RecyclerView.ViewHolder {
        TextView merchant, category, time, amount;
        TxVH(@NonNull View itemView) {
            super(itemView);
            merchant = itemView.findViewById(R.id.txMerchant);
            category = itemView.findViewById(R.id.txCategory);
            time = itemView.findViewById(R.id.txTime);
            amount = itemView.findViewById(R.id.txAmount);
        }
    }
}