package arnavigation.appsan.com.myapplication.ar;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import arnavigation.appsan.com.myapplication.PoiBrowserActivity;
import arnavigation.appsan.com.myapplication.R;

public class MyMarker {

    private double lat;
    private double longe;
private String info;
    private String title;
    private ViewRenderable LayoutRenderable;
    private CompletableFuture<ViewRenderable> theLayout;

    public MyMarker(double longe, double lat, String title, String info, Context c) {
        this.lat = lat;
        this.longe = longe;
        this.title = title;
this.info = info;
        theLayout = ViewRenderable.builder()
                .setView(c, R.layout.poi_container)
                .build();

        theLayout.handle(

                (notUsed, throwable) -> {
                    // When you build a Renderable, Sceneform loads its resources in the background while
                    // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                    // before calling get().

                    if (throwable != null) {
                        DemoUtils.displayError(c, "Unable to load renderables", throwable);
                        return null;
                    }

                    try {

                        LayoutRenderable = theLayout.get();

                        PoiBrowserActivity.hasFinishedLoading = true;

                    } catch (InterruptedException | ExecutionException ex) {
                        DemoUtils.displayError(c, "Unable to load renderables", ex);
                    }

                    return null;
                }
        );
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getLonge() {
        return longe;
    }

    public void setLonge(double longe) {
        this.longe = longe;
    }

    public ViewRenderable getLayoutRenderable() {
        return LayoutRenderable;
    }

    public void setLayoutRenderable(ViewRenderable layoutRenderable) {
        this.LayoutRenderable = layoutRenderable;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Node getTheView(Context c) {
        Node base = new Node();
        base.setRenderable(LayoutRenderable);
/*
        // Add  listeners etc here
        View eView = LayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched." + ((TextView) v.findViewById(R.id.poi_container_name)).getText(), Toast.LENGTH_LONG)
                    .show();



            return false;
        });
*/
        return base;
    }

}
