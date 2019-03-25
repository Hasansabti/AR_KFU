// Generated code from Butter Knife. Do not modify!
package arnavigation.appsan.com.myapplication;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import java.lang.IllegalStateException;
import java.lang.Override;

public class MapsActivity_ViewBinding implements Unbinder {
  private MapsActivity target;

  @UiThread
  public MapsActivity_ViewBinding(MapsActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public MapsActivity_ViewBinding(MapsActivity target, View source) {
    this.target = target;

    target.fab_menu = Utils.findRequiredViewAsType(source, R.id.fab_menu_btn, "field 'fab_menu'", FloatingActionMenu.class);
    target.ar_nav_btn = Utils.findRequiredViewAsType(source, R.id.ar_nav_btn, "field 'ar_nav_btn'", FloatingActionButton.class);
    target.poi_browser_btn = Utils.findRequiredViewAsType(source, R.id.poi_browser_btn, "field 'poi_browser_btn'", FloatingActionButton.class);
    target.decode_editText = Utils.findRequiredViewAsType(source, R.id.decode_box, "field 'decode_editText'", EditText.class);
    target.decode_button = Utils.findRequiredViewAsType(source, R.id.decode_btn, "field 'decode_button'", Button.class);
    target.progressBar = Utils.findRequiredViewAsType(source, R.id.progressBar_maps, "field 'progressBar'", ProgressBar.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    MapsActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.fab_menu = null;
    target.ar_nav_btn = null;
    target.poi_browser_btn = null;
    target.decode_editText = null;
    target.decode_button = null;
    target.progressBar = null;
  }
}
