package cycredit.io;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Helper class to manage PlayStation-style bottom navigation
 * Handles tab label updates and smooth animations
 */
public class PsBottomNavHelper {
    
    private BottomNavigationView bottomNav;
    private TextView tabLabel;
    private int currentTabId = -1;
    
    public PsBottomNavHelper(BottomNavigationView bottomNav, TextView tabLabel) {
        this.bottomNav = bottomNav;
        this.tabLabel = tabLabel;
    }
    
    /**
     * Update the tab label text based on selected item
     */
    public void updateTabLabel(int itemId) {
        if (tabLabel == null) return;
        
        String label = getLabelForItem(itemId);
        if (label != null && !label.equals(tabLabel.getText().toString())) {
            // Animate label change
            animateLabelChange(label);
        }
    }
    
    /**
     * Get label text for menu item
     */
    private String getLabelForItem(int itemId) {
        if (itemId == R.id.nav_play) {
            return "Play";
        } else if (itemId == R.id.nav_finance) {
            return "Finance";
        } else if (itemId == R.id.nav_guilds) {
            return "Guilds";
        } else if (itemId == R.id.nav_profile) {
            return "Profile";
        }
        return null;
    }
    
    /**
     * Animate label fade out/in
     */
    private void animateLabelChange(String newLabel) {
        if (tabLabel == null) return;
        
        AnimatorSet animatorSet = new AnimatorSet();
        
        // Fade out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tabLabel, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        
        // Fade in with new text
        Runnable changeText = () -> {
            tabLabel.setText(newLabel);
            tabLabel.setAlpha(0f);
        };
        
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tabLabel, "alpha", 0f, 1f);
        fadeIn.setDuration(150);
        fadeIn.setStartDelay(50);
        
        animatorSet.playSequentially(fadeOut, fadeIn);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                changeText.run();
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {}
            
            @Override
            public void onAnimationCancel(Animator animation) {}
            
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        
        animatorSet.start();
    }
    
    /**
     * Setup navigation listener with label updates
     */
    public void setupNavigation(BottomNavigationView.OnNavigationItemSelectedListener baseListener) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Update label
            updateTabLabel(itemId);
            
            // Call base listener
            if (baseListener != null) {
                return baseListener.onNavigationItemSelected(item);
            }
            
            return true;
        });
    }
    
    /**
     * Set selected item and update label
     */
    public void setSelectedItem(int itemId) {
        bottomNav.setSelectedItemId(itemId);
        updateTabLabel(itemId);
    }
}

