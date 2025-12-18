package cycredit.io;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RoomCanvasView extends FrameLayout {

    private static final int GRID_COLUMNS = 8;
    private static final int GRID_ROWS = 6;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RoomItem> items = new ArrayList<>();
    private OnItemInteractionListener listener;

    public RoomCanvasView(Context context) {
        super(context);
        init();
    }

    public RoomCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        gridPaint.setColor(Color.parseColor("#40FFFFFF")); // Light grid lines on dark bg
        gridPaint.setStrokeWidth(2f);
    }

    public void setItems(List<RoomItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        render();
    }

    public void setOnItemInteractionListener(OnItemInteractionListener listener) {
        this.listener = listener;
    }

    private void render() {
        removeAllViews();
        if (getWidth() == 0 || getHeight() == 0) {
            post(this::render);
            return;
        }

        int cellWidth = getWidth() / GRID_COLUMNS;
        int cellHeight = getHeight() / GRID_ROWS;

        for (RoomItem item : items) {
            TextView view = buildItemView(item);
            LayoutParams params = new LayoutParams(cellWidth, cellHeight);
            params.leftMargin = item.x * cellWidth;
            params.topMargin = item.y * cellHeight;
            view.setRotation(item.rotation);
            addView(view, params);
        }
    }

    private TextView buildItemView(RoomItem item) {
        TextView tv = new TextView(getContext());
        tv.setText(shortCode(item.itemCode));
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setBackgroundResource(item.starter ? R.drawable.room_item_starter_bg : R.drawable.room_item_bg);
        tv.setOnClickListener(v -> {
            if (listener != null) listener.onItemClicked(item);
        });
        return tv;
    }

    private String shortCode(String code) {
        if (code == null) return "";
        String[] parts = code.split("_");
        String candidate = parts[0];
        if (candidate.length() > 4) {
            candidate = candidate.substring(0, 4);
        }
        return candidate.toUpperCase();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cellWidth = getWidth() / GRID_COLUMNS;
        int cellHeight = getHeight() / GRID_ROWS;

        for (int c = 1; c < GRID_COLUMNS; c++) {
            float x = c * cellWidth;
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        for (int r = 1; r < GRID_ROWS; r++) {
            float y = r * cellHeight;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    public interface OnItemInteractionListener {
        void onItemClicked(RoomItem item);
    }

    public static class RoomItem {
        public final long id;
        public final String itemCode;
        public final int x;
        public final int y;
        public final int rotation;
        public final int z;
        public final boolean starter;

        public RoomItem(long id, String itemCode, int x, int y, int rotation, int z, boolean starter) {
            this.id = id;
            this.itemCode = itemCode;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.z = z;
            this.starter = starter;
        }
    }
}

