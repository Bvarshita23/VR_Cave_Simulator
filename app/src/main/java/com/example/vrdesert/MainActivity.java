package com.example.vrdesert;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.opengl.GLSurfaceView;
import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements InteractionManager.InteractionListener {

    public enum VRState {
        WELCOME,
        LANDING,
        TRANSITIONING,
        INSIDE_CAVE
    }

    // ── GL / VR ────────────────────────────────────────────────────────────
    private GLSurfaceView glSurfaceView;
    private VRRenderer vrRenderer;
    private SensorHandler sensorHandler;
    private MoveServer moveServer;
    private AudioEngine audioEngine;

    // ── UI ─────────────────────────────────────────────────────────────────
    private TextView movesCounter;
    private TextView sceneLabel;
    private View sceneTransitionOverlay;
    private View infoCardContainer;
    private TextView infoCardTitle;
    private TextView infoCardBody;

    // ── State management ──────────────────────────────────────────────────
    private VRState currentState = VRState.WELCOME;
    private int selectedCaveIndex = -1;
    private boolean transitioning = false;
    private TextToSpeech tts;

    // ── Audio ──────────────────────────────────────────────────────────────
    private MediaPlayer narrationPlayer;
    private MediaPlayer menuMusicPlayer;
    private Handler stateHandler = new Handler(Looper.getMainLooper());

    // ── Scene data ─────────────────────────────────────────────────────────
    private static final String[] CAVE_NAMES = {
            "Ajanta Caves",
            "Badami Caves",
            "Borra Caves",
            "Belum Caves",
            "Udayagiri Caves",
            "Barabar Caves"
    };

    private static final String[] CAVE_STATES = {
            "Maharashtra",
            "Karnataka",
            "Andhra Pradesh",
            "Andhra Pradesh",
            "Odisha",
            "Bihar"
    };

    private static final String[] NARRATION_TEXTS = {
            "The Ajanta Caves in Maharashtra are a series of 29 Buddhist cave monuments dating from the 2nd century BCE. They represent some of the finest examples of ancient Indian art and architecture.",
            "The Badami cave temples in Karnataka are a complex of Hindu, Jain, and possibly Buddhist cave temples. They are famous for their rock-cut architecture dating back to the 6th century.",
            "Borra Caves in Andhra Pradesh are located in the Ananthagiri hills and are one of the deepest caves in India. They are known for their magnificent stalactite and stalagmite formations.",
            "The Udayagiri and Khandagiri Caves in Odisha are partly natural and partly artificial caves of archaeological, historical and religious importance dating back to the 1st century BCE."
    };

    // Insight data: [scene 0‑3][button 0‑2][title, body]
    private static final String[][][] INSIGHTS = {
            {
                    { "❄ How Tunnels Form",
                            "This perfectly oval passage was carved not by hand but by meltwater boring through glacial ice over decades. Liquid water always finds the smallest weakness in solid ice — drilling downward and outward until a passage like this forms." },
                    { "💡 The Light Ahead",
                            "That circle of daylight at the tunnel's far end is the strongest magnet in any ice cave. Ice transmits blue wavelengths most efficiently, which is why the glow has an ethereal, almost supernatural quality compared to ordinary sunlight." },
                    { "🧊 Temperature Drop",
                            "As you step inside, the temperature drops 10–15°C within a few metres. The ice surrounding you acts as an insulator far more effective than concrete — maintaining a near-constant subzero environment regardless of what the weather is doing outside." }
            },
            {
                    { "🏔 Frozen Waterfalls",
                            "What you see here are called ice curtains or frozen waterfalls. When water seeps through cracks in the rock above and meets subzero cave air, it freezes mid-flow. Each column grew one freeze-cycle at a time — some of these formations are decades old." },
                    { "🪨 Rock Behind Ice",
                            "Notice the dark layered rock visible behind the ice. These are sedimentary strata — each horizontal band represents thousands of years of geological sedimentation. The ice clinging to them is a newcomer; the rock itself may be 400 million years old." },
                    { "⚖ Weight of Ice",
                            "The largest ice curtain columns you see can weigh over one tonne each. Despite their delicate, translucent appearance, they are structurally dense — the ice near the base is compressed so hard that it contains almost no air bubbles at all." }
            },
            {
                    { "💧 Icicle Growth",
                            "Each single icicle here grows roughly 1 centimetre per day under ideal conditions. A water droplet arrives, partially freezes, and leaves a thin ice ring before the next drop comes. The largest ones here may have been growing for 10 years or more." },
                    { "🔵 Why Blue Light?",
                            "The ethereal blue-grey light filtering through the ceiling comes from the physics of glacial ice. Dense ice absorbs red and yellow wavelengths and lets only blue pass through. The deeper and denser the ice, the purer and more intense the blue." },
                    { "🤫 Total Silence",
                            "Ice caves are among the quietest places on Earth. The ice and rock around you absorb nearly all sound — no echo, no ambient hum. Scientists have measured near-zero decibel levels in chambers like this. Many first-time visitors hear their own heartbeat for the first time." }
            },
            {
                    { "🪟 The Portal Effect",
                            "This view — looking out from the cave's inner chamber — is one of the most photographed compositions in glacial photography. The blue glacial ice frames the external world like a stained-glass window, creating a stark contrast between frozen stillness inside and life outside." },
                    { "🌋 Outside the Ice",
                            "What you see beyond the cave mouth is Iceland's volcanic rock plain, covered in a thin frost layer. The glacier you are standing inside sits directly atop ancient lava flows — a river of compacted ice, thousands of years old, moving millimetres per day toward the sea." },
                    { "⏳ Ice as History",
                            "The ice forming the ceiling above you may be 500–1,000 years old. Climate scientists drill ice cores from glaciers like this to study ancient atmospheres — each compressed layer is a year of snowfall, preserving bubbles of air from centuries gone by." }
            }
    };

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);

        sensorHandler = new SensorHandler(this);
        InteractionManager interactionManager = new InteractionManager(this);

        vrRenderer = new VRRenderer(this, sensorHandler, interactionManager);
        vrRenderer.setCaveData(CAVE_NAMES);
        glSurfaceView.setRenderer(vrRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        movesCounter = findViewById(R.id.movesCounter);
        sceneLabel = findViewById(R.id.sceneLabel);
        sceneTransitionOverlay = findViewById(R.id.sceneTransitionOverlay);
        infoCardContainer = findViewById(R.id.infoCardContainer);
        infoCardTitle = findViewById(R.id.infoCardTitle);
        infoCardBody = findViewById(R.id.infoCardBody);

        updateSceneLabel();

        Button btnClose = findViewById(R.id.btnCloseInfo);
        if (btnClose != null)
            btnClose.setOnClickListener(v -> hideInfoCard());

        audioEngine = new AudioEngine(this);

        // Initialize TTS with improved welcome message and explicit success check
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                stateHandler.postDelayed(() -> {
                    tts.speak("Welcome to the Indian Cave Explorer. Gaze at the button to start your journey.", TextToSpeech.QUEUE_FLUSH, null, "WELCOME_MSG");
                }, 2000);
            }
        });
    }

    private void playNarration(int index) {
        String name = CAVE_NAMES[index % CAVE_NAMES.length];
        String state = CAVE_STATES[index % CAVE_STATES.length];
        String speechText = name + ", " + state;
        if (tts != null) {
            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        Toast.makeText(this, "Selected: " + name, Toast.LENGTH_SHORT).show();
    }

    private void playFullHistory(int index) {
        if (narrationPlayer != null) {
            narrationPlayer.release();
        }
        String history = NARRATION_TEXTS[index % NARRATION_TEXTS.length];
        if (tts != null) {
            tts.speak(history, TextToSpeech.QUEUE_ADD, null, null);
        }

        stateHandler.postDelayed(() -> {
            onNarrationFinished();
        }, 15000);
    }

    private void onNarrationFinished() {
        if (currentState == VRState.INSIDE_CAVE) {
            stateHandler.postDelayed(() -> {
                returnToLanding();
            }, 3000);
        }
    }

    private void returnToLanding() {
        if (transitioning)
            return;
        transitioning = true;

        sceneTransitionOverlay.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(sceneTransitionOverlay, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();

        stateHandler.postDelayed(() -> {
            currentState = VRState.LANDING;
            vrRenderer.setState(VRRenderer.State.LANDING);

            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(sceneTransitionOverlay, "alpha", 1f, 0f);
            fadeOut.setDuration(500);
            fadeOut.start();
            stateHandler.postDelayed(() -> {
                sceneTransitionOverlay.setVisibility(View.GONE);
                transitioning = false;
            }, 500);
        }, 600);
    }

    public void enterCave(int caveIndex) {
        if (transitioning)
            return;
        selectedCaveIndex = caveIndex;
        transitioning = true;

        sceneTransitionOverlay.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(sceneTransitionOverlay, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.start();

        stateHandler.postDelayed(() -> {
            currentState = VRState.INSIDE_CAVE;
            vrRenderer.setCaveScene(caveIndex);

            playFullHistory(caveIndex);
            sceneLabel.setText(CAVE_NAMES[caveIndex]);

            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(sceneTransitionOverlay, "alpha", 1f, 0f);
            fadeOut.setDuration(500);
            fadeOut.start();
            stateHandler.postDelayed(() -> {
                sceneTransitionOverlay.setVisibility(View.GONE);
                transitioning = false;
            }, 500);
        }, 600);
    }

    private void showInsight(int buttonIndex) {
        String[] data = INSIGHTS[selectedCaveIndex != -1 ? selectedCaveIndex : 0][buttonIndex];
        infoCardTitle.setText(data[0]);
        infoCardBody.setText(data[1]);

        infoCardContainer.setAlpha(0f);
        infoCardContainer.setVisibility(View.VISIBLE);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(infoCardContainer, "alpha", 0f, 1f);
        fadeIn.setDuration(250);
        fadeIn.start();
    }

    private void hideInfoCard() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(infoCardContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.start();
        infoCardContainer.postDelayed(() -> {
            infoCardContainer.setVisibility(View.GONE);
            if (vrRenderer != null) {
                vrRenderer.resetInfoButtons();
            }
        }, 200);
    }

    private void updateSceneLabel() {
        if (selectedCaveIndex != -1) {
            sceneLabel.setText(CAVE_NAMES[selectedCaveIndex]);
        } else {
            sceneLabel.setText("Cave Selection Menu");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        sensorHandler.start();
        if (audioEngine != null) {
            audioEngine.startCaveAmbiance();
        }
        startAudioYawUpdater();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        sensorHandler.stop();
        if (audioEngine != null) {
            audioEngine.stopAudio();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moveServer != null)
            moveServer.stopServer();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onGazeStarted(int objectId) {
        if (currentState == VRState.LANDING) {
            vrRenderer.highlightCard(objectId, true);
        }
    }

    @Override
    public void onGazeEnded(int objectId) {
        if (currentState == VRState.LANDING) {
            vrRenderer.highlightCard(objectId, false);
        }
    }

    @Override
    public void onNarrationTriggered(int caveId) {
        if (currentState == VRState.LANDING) {
            playNarration(caveId);
        }
    }

    @Override
    public void onTransitionTriggered(int caveId) {
        if (currentState == VRState.WELCOME) {
            currentState = VRState.LANDING;
            vrRenderer.setState(VRRenderer.State.LANDING);
        } else if (currentState == VRState.LANDING) {
            enterCave(caveId);
        }
    }

    @Override
    public void onObjectCollected(int objectId, GameObject.Type type) {
    }

    private android.os.Handler audioHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable audioYawRunnable;

    private void startAudioYawUpdater() {
        audioYawRunnable = () -> {
            if (audioEngine != null && sensorHandler != null) {
                audioEngine.setHeadYaw(sensorHandler.getYaw());
            }
            audioHandler.postDelayed(audioYawRunnable, 100);
        };
        audioHandler.post(audioYawRunnable);
    }
}
