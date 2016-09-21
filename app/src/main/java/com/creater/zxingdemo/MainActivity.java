package com.creater.zxingdemo;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;




public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback2,MainViewInterface,View.OnClickListener {


    SurfaceView mainSurface;

    TextView resultTextview;

    Button okButton;


    private SurfaceHolder mSurfaceHolder;
    private MainPresenter mPresenter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mainSurface=(SurfaceView)this.findViewById(R.id.main_surface);
        okButton=(Button)this.findViewById(R.id.ok_button);
        resultTextview=(TextView)this.findViewById(R.id.result_textview);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        init();
    }

    private void init(){
        okButton.setOnClickListener(this);
        mPresenter=new MainPresenter(this,this);
        mSurfaceHolder=mainSurface.getHolder();
        mSurfaceHolder.addCallback(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPresenter.openCam(holder);
        //mPresenter.startChecking();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("xv","surface changed");
        mPresenter.closeCam();
        mPresenter.openCam(holder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("xv","surface destoryed");
    }

    @Override
    public void checking() {
        resultTextview.setText("正在检测中");
    }

    @Override
    public void checkSuccess(String decodeMsg) {
        resultTextview.setText(decodeMsg);
        mPresenter.closeCam();
        mPresenter.openCam(mSurfaceHolder);
    }

    @Override
    public void timeOut() {
        resultTextview.setText("没有检测对象");
        mPresenter.closeCam();
        mPresenter.openCam(mSurfaceHolder);
    }

    @Override
    public void onClick(View v) {
        mPresenter.startChecking();
    }
}
