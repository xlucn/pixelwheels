package com.greenyetilab.race;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class RaceGameScreen extends ScreenAdapter {
    private static final float MAX_PITCH = 30;
    private static final float MAX_ACCEL = 7;
    private static final float TIME_STEP = 1f/60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private final RaceGame mGame;
    private final World mWorld;
    private Batch mBatch;

    private MapInfo mMapInfo;
    private TiledMap mMap;
    private Car mCar;

    private GameRenderer mGameRenderer;

    private Stage mHudStage;
    private ScreenViewport mHudViewport;
    private WidgetGroup mHud;
    private Label mTimeLabel;
    private float mTime = 0;

    public RaceGameScreen(RaceGame game, MapInfo mapInfo) {
        mGame = game;
        mBatch = new SpriteBatch();
        mWorld = new World(new Vector2(0, 0), true);
        setupMap(mapInfo);
        setupHud();
        mGameRenderer = new GameRenderer(game, mWorld, mMap, mBatch);
        mCar = mGameRenderer.getCar();
    }

    void setupMap(MapInfo mapInfo) {
        mMapInfo = mapInfo;
        mMap = mapInfo.getMap();
    }

    void setupHud() {
        mHudViewport = new ScreenViewport();
        mHudStage = new Stage(mHudViewport, mBatch);
        Gdx.input.setInputProcessor(mHudStage);

        Skin skin = mGame.getAssets().skin;
        mHud = new WidgetGroup();

        mTimeLabel = new Label("0:00.0", skin);
        mTimeLabel.invalidate();
        mHud.addActor(mTimeLabel);
        mHud.setHeight(mTimeLabel.getHeight());

        mHudStage.addActor(mHud);
        updateHud();
    }

    private float mTimeAccumulator = 0;

    private void doPhysicsStep(float deltaTime) {
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        float frameTime = Math.min(deltaTime, 0.25f);
        mTimeAccumulator += frameTime;
        while (mTimeAccumulator >= TIME_STEP) {
            mWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            mTimeAccumulator -= TIME_STEP;
        }
    }

    @Override
    public void render(float delta) {
        mTime += delta;

        doPhysicsStep(delta);
        mHudStage.act(delta);
        mCar.act(delta);
        switch (mCar.getState()) {
        case RUNNING:
            break;
        case BROKEN:
            mGame.showGameOverOverlay(mMapInfo);
            return;
        case FINISHED:
            mGame.showFinishedOverlay(mMapInfo, mTime);
            return;
        }

        handleInput();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        mGameRenderer.render();

        updateHud();
        mHudStage.draw();
    }

    private void updateHud() {
        String text = StringUtils.formatRaceTime(mTime);
        mTimeLabel.setText(text);
        mHud.setPosition(5, mHudViewport.getScreenHeight() - mHud.getHeight() - 15);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mHudViewport.update(width, height, true);
        mGameRenderer.onScreenResized();
    }

    private void handleInput() {
        float direction = 0;
        if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Compass)) {
            float angle = Gdx.input.getPitch();
            direction = MathUtils.clamp(angle, -MAX_PITCH, MAX_PITCH) / MAX_PITCH;

        } else if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
            float angle = -Gdx.input.getAccelerometerY();
            direction = MathUtils.clamp(angle, -MAX_ACCEL, MAX_ACCEL) / MAX_ACCEL;
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                direction = 1;
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                direction = -1;
            }
        }
        mCar.setDirection(direction);
        boolean accelerating = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || touchAt(0.5f, 1);
        boolean braking = Gdx.input.isKeyPressed(Input.Keys.SPACE) || touchAt(0, 0.5f);
        mCar.setAccelerating(accelerating);
        mCar.setBraking(braking);
    }

    private boolean touchAt(float startX, float endX) {
        // check if any finger is touching the area between startX and endX
        // startX/endX are given between 0 (left edge of the screen) and 1 (right edge of the screen)
        for (int i = 0; i < 2; i++) {
            float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
            if (Gdx.input.isTouched(i) && (x >= startX && x <= endX)) {
                return true;
            }
        }
        return false;
    }
}
