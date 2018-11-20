package com.nabto.nabtovideo;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.R.anim;
import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nabto.NabtoBaseActivity;
import com.nabto.api.NabtoAndroidAssetManager;
import com.nabto.api.NabtoApi;
import com.nabto.api.NabtoTunnelState;
import com.nabto.api.Tunnel;
import com.nabto.api.TunnelInfoResult;
import com.nabto.nabtovideo.util.Storage;
import com.nabto.nabtovideo.util.SwipeDismissListViewTouchListener;
import com.nabto.nabtovideo.util.TunnelManager;
import com.nabto.nabtovideo.util.VideoDevice;
import com.nabto.nabtovideo.util.VideoDevice.VideoType;
import com.nabto.qr.IntentIntegrator;
import com.nabto.qr.IntentResult;

import com.nabto.nabtovideo.R;

public class MainActivity extends NabtoBaseActivity {
	
	private NabtoApi nabtoApi;
	private Tunnel tunnel;

    private String email;
    private String password;
	
	private Timer timer;
	private VideoDevice activeDevice;

    private boolean videoViewerVisible;

    private ProgressDialog progress;

    public final static String NABTOVERSION = "com.nabto.api.NABTOVERSION";
    static final int ADD_DEVICE_INTENT = 1;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        email = "guest";
        password = "";
        initializeNabto();

        progress = new ProgressDialog(this);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment(), "ListViewFragment").commit();
		}

        Uri data = getIntent().getData();
        if (data != null) {
            Log.d(this.getClass().getName(), "Opened from external URI: " + data.toString());
            Storage storage = new Storage();
            storage.addDeviceFromUriString(getApplicationContext(), data.toString());
        }
	}

    private void initializeNabto() {
        nabtoApi = new NabtoApi(new NabtoAndroidAssetManager(this));
        nabtoApi.startup();
        TunnelManager.instance().initialize(nabtoApi, email, password);
        Storage storage = new Storage();
        ArrayList devices = storage.getFavorites(this);
        TunnelManager.instance().populateConnectionCache(devices);
    }

    private void deinitializeNabto() {
        TunnelManager.instance().deinitialize();
        nabtoApi.shutdown();
        nabtoApi = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(this.getClass().getName(), "On Resume");

        progress.dismiss();
        closeVideo();

        videoViewerVisible = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(this.getClass().getName(), "On Pause");
        if (!videoViewerVisible) {
            closeVideo();
        }
    }

    @Override
    protected void onAppResumeFromBackground() {
        Log.d(this.getClass().getName(), "onAppResumeFromBackground");
        initializeNabto();
    }

    @Override
    protected void onAppSuspend() {
        Log.d(this.getClass().getName(), "onAppSuspend");
        deinitializeNabto();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(this.getClass().getName(), "On result. Refreshing list.");

        if (resultCode == RESULT_OK) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null) {
                Storage storage = new Storage();
                storage.addDeviceFromUriString(getApplicationContext(), scanResult.getContents());
            }
        }

        PlaceholderFragment fragment = (PlaceholderFragment)getFragmentManager().findFragmentByTag("ListViewFragment");
        fragment.reloadList();
    }
    
    private void startVideo(NabtoTunnelState state) {
        timer.cancel();

        // Rapid close/startup of application results in a closed tunnel
        // when the checkConnectionState timer triggers
        if (tunnel == null) {
            return;
        }

        switch (activeDevice.type) {
        case VideoType.MPEG:
            startPlayerVideo(state);
            break;
        case VideoType.WEB:
            startWebVideo();
            break;
        default:
            Log.e(this.getClass().getName(), "Unknown video device type");
            break;
        }
    }
    
    private void startWebVideo() {
        String baseUrl = "http://localhost:" + nabtoApi.tunnelInfo(tunnel).getPort();
        if (!activeDevice.url.startsWith("/")) {
            baseUrl += "/";
        }
        String url = baseUrl + activeDevice.url;

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(VideoDevice.DEVICEURL, url);
        startActivity(intent);
        overridePendingTransition(anim.slide_in_left, anim.slide_out_right);
    }
    
    private void startPlayerVideo(NabtoTunnelState state) {
//        String baseUrl = "rtsp://admin:MBHPr5hObPfM@localhost:" + nabtoApi.tunnelInfo(tunnel).getPort(); /* y-cam test */
        String baseUrl = "rtsp://127.0.0.1:" + nabtoApi.tunnelInfo(tunnel).getPort();
        if (!activeDevice.url.startsWith("/")) {
            baseUrl += "/";
        }
        String url = baseUrl + activeDevice.url;

        videoViewerVisible = true;
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoDevice.DEVICEURL, url);
        intent.putExtra("connection_type", state.toString());
        startActivity(intent);
        overridePendingTransition(anim.slide_in_left, anim.slide_out_right);
    }
    
    private void closeVideo() {
        if (timer != null) {
            timer.cancel();
        }
        if (tunnel != null) {
            TunnelManager.instance().releaseTunnel(tunnel);
            tunnel = null;
        }
    }
    
    private void checkConnectionState(Tunnel tunnel) {
        TunnelInfoResult tunnelInfoResult = nabtoApi.tunnelInfo(tunnel);
        final NabtoTunnelState state = tunnelInfoResult.getTunnelState();
        Log.v(this.getClass().getName(), "Tunnel status for " + tunnel + ": " + getStateMessage(tunnel));

        Handler handler;
        Runnable runnable;

        switch (state) {
            case CONNECTING:
                break;
            case READY_FOR_RECONNECT:   
            case LOCAL:
            case REMOTE_P2P:
            case REMOTE_RELAY:
            case REMOTE_RELAY_MICRO:
                timer.cancel();
                handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                        startVideo(state);
                    }
                };
                handler.postDelayed(runnable, 0);
                break;
            default:
                timer.cancel();
                progress.setMessage(getStateMessage(tunnel));
                closeVideo();
                TunnelManager.instance().closeTunnel(tunnel);
                handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        progress.dismiss();
                    }
                };
                handler.postDelayed(runnable, 2500);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void editDevice(VideoDevice device) {
        Intent intent = new Intent(this, AddDeviceActivity.class);
        intent.putExtra(VideoDevice.DEVICETITLE, device.title);
        intent.putExtra(VideoDevice.DEVICENAME, device.name);
        intent.putExtra(VideoDevice.DEVICEPORT, device.port);
        intent.putExtra(VideoDevice.DEVICEHOST, device.host);
        intent.putExtra(VideoDevice.DEVICEURL, device.url);
        intent.putExtra(VideoDevice.DEVICETYPE, device.type);
        startActivityForResult(intent, ADD_DEVICE_INTENT);
    }
    
    public void openVideo(VideoDevice device) {
        if (tunnel != null) {
            // in some monkey testing scenarios, the closeVideo function appears to not have
            // been called prior to opening a new tunnel, cleanup to prevent being stuck with
            // a stale tunnel in a bad state
            TunnelManager.instance().releaseTunnel(tunnel);
            TunnelManager.instance().closeTunnel(tunnel);
        }
        
        activeDevice = device;
        tunnel = TunnelManager.instance().openTunnel(activeDevice);

		if (timer != null) {
			timer.cancel();
		}
		
		timer = new Timer();
        TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
                final String message = getStateMessage(tunnel);
				runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                    if (message != null) {
                            progress.setMessage(message);
                            progress.show();
                        }
                        checkConnectionState(tunnel);
	                }
	            });
			}
             };
        timer.schedule(timerTask, 0, 100);
	}

    private String getStateMessage(Tunnel tunnel) {
        NabtoTunnelState state = nabtoApi.tunnelInfo(tunnel).getTunnelState();
        final String message;
        switch (state) {
            case CLOSED:
                message = getErrorMessage(tunnel);
                break;
            case CONNECTING:
                message = "Connecting";
                break;
            case READY_FOR_RECONNECT:
                message = "Connection was lost, please retry";
                break;
            case LOCAL:
            case REMOTE_P2P:
            case REMOTE_RELAY:
            case REMOTE_RELAY_MICRO:
                message = null;
                break;
            case UNKNOWN:
            default:
                message = "State unknown, probing";
                break;
        }
        return message;
    }

    private String getErrorMessage(Tunnel tunnel) {
        // TODO ... improve error handling at wrapper level
        TunnelInfoResult tunnelInfoResult = nabtoApi.tunnelInfo(tunnel);
        int error = tunnelInfoResult.getLastError();
        switch (error) {
            case 1000015:
            case 1000026:
                return "Device is offline";
            case 1000011:
                return "Accessed denied";
            default:
                return "An error occurred, please try again later (" + error + ")";
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int i = item.getItemId();
        if (i == R.id.action_add) {
            intent = new Intent(this, AddDeviceActivity.class);
            startActivityForResult(intent, ADD_DEVICE_INTENT);
            return true;
        }
        else if (i == R.id.action_qr) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
            return true;
        }
        else if (i == R.id.action_about) {
            intent = new Intent(this, AboutActivity.class);
            String versionString = "Nabto Client version: " + nabtoApi.version();
            intent.putExtra(NABTOVERSION, versionString);
            startActivity(intent);
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends ListFragment {

        private ArrayAdapter<String> adapter;
        private ArrayList<VideoDevice> devices;

        public PlaceholderFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList()) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                    text1.setText(devices.get(position).getTitle());
                    text2.setText(devices.get(position).getTypeString());
                    return view;
                }
            };

            populateAdapter();
            setListAdapter(adapter);

            return rootView;
        }

        private void removeItem(int position) {
            Storage storage = new Storage();
            storage.removeFavorite(getActivity(), devices.get(position));
            reloadList();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            ListView listView = getListView();
            registerForContextMenu(listView);

            // Create a ListView-specific touch listener. ListViews are given special treatment because
            // by default they handle touches for their list items... i.e. they're in charge of drawing
            // the pressed state (the list selector), handling list item clicks, etc.
            SwipeDismissListViewTouchListener touchListener =
                    new SwipeDismissListViewTouchListener(
                            listView,
                            new SwipeDismissListViewTouchListener.DismissCallbacks() {
                                @Override
                                public boolean canDismiss(int position) {
                                    return true;
                                }

                                @Override
                                public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                    for (int position : reverseSortedPositions) {
                                        removeItem(position);
                                    }
                                }
                            });

            listView.setOnTouchListener(touchListener);
            // Setting this scroll listener is required to ensure that during ListView scrolling,
            // we don't look for swipes.
            listView.setOnScrollListener(touchListener.makeScrollListener());
        }

        private void populateAdapter() {
            Storage storage = new Storage();
            devices = storage.getFavorites(this.getActivity());

            adapter.clear();

            for (VideoDevice device : devices) {
                adapter.add(device.getTitle());
            }
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Activity activity = getActivity();

            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openVideo(devices.get(position));
            } else {
                Log.e(this.getClass().getName(), "Invalid cast of MainActivity from fragment");
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);

            MenuInflater inflater = this.getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_listitem, menu);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            int position = info.position;

            int i = item.getItemId();
            if (i == R.id.action_remove) {
                removeItem(position);
                return true;
            } else if (i == R.id.action_edit) {
                ((MainActivity) getActivity()).editDevice(devices.get(position));
                return true;
            } else {
                return super.onContextItemSelected(item);
            }
        }

        public void reloadList() {
            populateAdapter();
            adapter.notifyDataSetChanged();
        }
    }

}
