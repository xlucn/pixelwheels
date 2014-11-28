package com.greenyetilab.race;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.greenyetilab.utils.log.NLog;

/**
 * Responsible for rendering the game world
 */
public class GameRenderer {
    private static final float VIEWPORT_WIDTH = 40;

    private static final boolean DEBUG_RENDERER = true;

    private final TiledMap mMap;
    private final OrthogonalTiledMapRenderer mRenderer;
    private final Box2DDebugRenderer mDebugRenderer;
    private final Batch mBatch;
    private final OrthographicCamera mCamera;
    private final ShapeRenderer mShapeRenderer = new ShapeRenderer();
    private final World mWorld;
    private final RaceGame mGame;
    private final float mMapWidth;
    private final float mMapHeight;


    private Car mCar;

    public GameRenderer(RaceGame game, World world, TiledMap map, Batch batch) {
        mDebugRenderer = new Box2DDebugRenderer();
        mGame = game;
        mWorld = world;

        mMap = map;
        TiledMapTileLayer layer = (TiledMapTileLayer) mMap.getLayers().get(0);
        mMapWidth = Constants.UNIT_FOR_PIXEL * layer.getWidth() * layer.getTileWidth();
        mMapHeight = Constants.UNIT_FOR_PIXEL * layer.getHeight() * layer.getTileHeight();

        mBatch = batch;
        mCamera = new OrthographicCamera();
        mRenderer = new OrthogonalTiledMapRenderer(mMap, Constants.UNIT_FOR_PIXEL, mBatch);

        setupCar();
    }

    public void render() {
        updateCamera();

        mRenderer.setView(mCamera);
        mRenderer.render();

        mBatch.setProjectionMatrix(mCamera.combined);
        mBatch.begin();
        mCar.draw(mBatch);
        mBatch.end();

        if (DEBUG_RENDERER) {
            mShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            mShapeRenderer.setColor(1, 1, 1, 1);
            mShapeRenderer.setProjectionMatrix(mCamera.combined);
            TiledMapTileLayer layer = (TiledMapTileLayer) mMap.getLayers().get(0);
            float tileW = Constants.UNIT_FOR_PIXEL * layer.getTileWidth();
            float tileH = Constants.UNIT_FOR_PIXEL * layer.getTileHeight();
            for (float y = 0; y < mMapHeight; y += tileH) {
                for (float x = 0; x < mMapWidth; x += tileW) {
                    mShapeRenderer.rect(x, y, Constants.UNIT_FOR_PIXEL, Constants.UNIT_FOR_PIXEL);
                }
            }
            mShapeRenderer.setColor(0, 0, 1, 1);
            mShapeRenderer.rect(mCar.getX(), mCar.getY(), Constants.UNIT_FOR_PIXEL, Constants.UNIT_FOR_PIXEL);
            mShapeRenderer.end();

            mDebugRenderer.render(mWorld, mCamera.combined);
        }
    }

    public Car getCar() {
        return mCar;
    }

    private void setupCar() {
        TiledMapTileLayer layer = (TiledMapTileLayer) mMap.getLayers().get(0);
        Vector2 position = findStartTilePosition(layer);
        assert(position != null);
        mCar = new Car(mGame, mWorld, layer, position);
    }

    private Vector2 findStartTilePosition(TiledMapTileLayer layer) {
        for (int ty=0; ty < layer.getHeight(); ++ty) {
            for (int tx=0; tx < layer.getWidth(); ++tx) {
                TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
                TiledMapTile tile = cell.getTile();
                if (tile.getProperties().containsKey("start")) {
                    float tw = Constants.UNIT_FOR_PIXEL * layer.getTileWidth();
                    float th = Constants.UNIT_FOR_PIXEL * layer.getTileHeight();
                    return new Vector2(tx * tw + tw / 2, ty * th + th / 2);
                }
            }
        }
        NLog.e("No Tile with 'start' property found");
        return null;
    }

    private void updateCamera() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        mCamera.viewportWidth = VIEWPORT_WIDTH;
        mCamera.viewportHeight = VIEWPORT_WIDTH * screenH / screenW;

        // Compute pos
        // FIXME: Take car speed into account when computing advance
        float advance = /*(mCar.getSpeed() / Car.MAX_SPEED) **/ Math.min(mCamera.viewportWidth, mCamera.viewportHeight) / 3;
        float x = mCar.getX() + advance * MathUtils.cosDeg(mCar.getAngle());
        float y = mCar.getY() + advance * MathUtils.sinDeg(mCar.getAngle());

        // Make sure we correctly handle boundaries
        float minX = mCamera.viewportWidth / 2;
        float minY = mCamera.viewportHeight / 2;
        float maxX = mMapWidth - mCamera.viewportWidth / 2;
        float maxY = mMapHeight - mCamera.viewportHeight / 2;

        if (mCamera.viewportWidth <= mMapWidth) {
            mCamera.position.x = MathUtils.clamp(x, minX, maxX);
        } else {
            mCamera.position.x = mMapWidth / 2;
        }
        if (mCamera.viewportHeight <= mMapHeight) {
            mCamera.position.y = MathUtils.clamp(y, minY, maxY);
        } else {
            mCamera.position.y = mMapHeight / 2;
        }
        mCamera.update();
    }

    public void onScreenResized() {
        updateCamera();
    }
}
