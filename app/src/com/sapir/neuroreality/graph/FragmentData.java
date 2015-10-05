
package com.sapir.neuroreality.graph;

import android.support.v4.app.FragmentActivity;

import com.sapir.neuroreality.R;

/**
 * Baseclass of all Activities of the Demo Application.
 * 
 * @author Philipp Jahoda
 */
public abstract class FragmentData extends FragmentActivity {

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.move_left_in_activity, R.anim.move_right_out_activity);
    }
}
