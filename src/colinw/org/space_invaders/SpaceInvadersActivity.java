package colinw.org.space_invaders;

import android.app.Activity;
import android.os.Bundle;

public class SpaceInvadersActivity extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    view_ = new SpaceInvadersView(this);

    setContentView(view_);

    view_.requestFocus();
  }

  @Override
  public void onBackPressed() {
    if (! view_.onBackPressed())
      super.onBackPressed();
  }

  private SpaceInvadersView view_;
}
