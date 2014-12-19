package com.greenyetilab.race;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.badlogic.gdx.utils.PerformanceCounters;
import com.greenyetilab.utils.log.NLog;

/**
 * Contains all the information and objects running in the world
 */
public class GameWorld implements ContactListener, Disposable {
    public enum State {
        RUNNING,
        BROKEN,
        FINISHED
    }

    private static final float TIME_STEP = 1f/60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private final MapInfo mMapInfo;

    private final World mBox2DWorld;
    private final RaceGame mGame;
    private final EnemySpawner mEnemySpawner;
    private float mTimeAccumulator = 0;

    private Vehicle mPlayerVehicle;
    private State mState = State.RUNNING;

    private Vector2[] mSkidmarks = new Vector2[4000];
    private int mSkidmarksIndex = 0;
    private Array<GameObject> mActiveGameObjects = new Array<GameObject>();
    private float mScore = 0;
    private float mBottomVisibleY = 0;
    private float mTopVisibleY = 0;

    private final PerformanceCounter mBox2DPerformanceCounter;
    private final PerformanceCounter mGameObjectPerformanceCounter;

    public GameWorld(RaceGame game, MapInfo mapInfo, PerformanceCounters performanceCounters) {
        mGame = game;
        mBox2DWorld = new World(new Vector2(0, 0), true);
        mBox2DWorld.setContactListener(this);
        mMapInfo = mapInfo;
        mEnemySpawner = new EnemySpawner(this, game.getAssets());

        mBox2DPerformanceCounter = performanceCounters.add("- box2d");
        mGameObjectPerformanceCounter = performanceCounters.add("- g.o");
        setupSled();
        setupOutsideWalls();
        setupObjects();
    }

    public MapInfo getMapInfo() {
        return mMapInfo;
    }

    public World getBox2DWorld() {
        return mBox2DWorld;
    }

    public Vehicle getPlayerVehicle() {
        return mPlayerVehicle;
    }

    public int getScore() {
        return (int)mScore;
    }

    public void increaseScore(int delta) {
        NLog.i("+%d", delta);
        mScore += delta;
    }

    public Vector2[] getSkidmarks() {
        return mSkidmarks;
    }

    public Array<GameObject> getActiveGameObjects() {
        return  mActiveGameObjects;
    }

    public void addGameObject(GameObject object) {
        mActiveGameObjects.add(object);
    }

    public void setVisibleSection(float bottom, float top) {
        mBottomVisibleY = bottom;
        mTopVisibleY = top;
    }

    public float getTopVisibleY() {
        return mTopVisibleY;
    }

    public float getBottomVisibleY() {
        return mBottomVisibleY;
    }

    public void act(float delta) {
        float oldY = mPlayerVehicle.getY();

        mBox2DPerformanceCounter.start();
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        float frameTime = Math.min(delta, 0.25f);
        mTimeAccumulator += frameTime;
        while (mTimeAccumulator >= TIME_STEP) {
            mBox2DWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            mTimeAccumulator -= TIME_STEP;
        }
        mBox2DPerformanceCounter.stop();

        float deltaY = mPlayerVehicle.getY() - oldY;
        if (delta > 0) {
            mScore += deltaY * Constants.SCORE_PER_METER;
            mEnemySpawner.setTopY(mTopVisibleY);
        }

        float outOfSightLimit = mBottomVisibleY - Constants.VIEWPORT_POOL_RECYCLE_HEIGHT;
        mGameObjectPerformanceCounter.start();
        for (int idx = mActiveGameObjects.size - 1; idx >= 0; --idx) {
            GameObject obj = mActiveGameObjects.get(idx);
            if (!obj.act(delta)) {
                mActiveGameObjects.removeIndex(idx);
                continue;
            }
            if (obj.getY() < outOfSightLimit && obj instanceof DisposableWhenOutOfSight) {
                ((DisposableWhenOutOfSight)obj).dispose();
                mActiveGameObjects.removeIndex(idx);
            }
        }
        mGameObjectPerformanceCounter.stop();
    }

    private void setupSled() {
        Vector2 position = mMapInfo.findStartTilePosition();
        assert(position != null);

        // Car
        TextureRegion carRegion = mGame.getAssets().findRegion("sled/sled");
        TextureRegion wheelRegion = mGame.getAssets().findRegion("sled/sled-ski");
        mPlayerVehicle = new Vehicle(carRegion, this, position);
        mPlayerVehicle.setPilot(new PlayerPilot(mGame.getAssets(), this, mPlayerVehicle));
        mPlayerVehicle.setLimitAngle(true);
        //mPlayerVehicle.setCorrectAngle(true);

        // Wheels
        final float REAR_WHEEL_Y = Constants.UNIT_FOR_PIXEL * 16f;
        final float WHEEL_BASE = Constants.UNIT_FOR_PIXEL * 46f;

        float wheelW = Constants.UNIT_FOR_PIXEL * wheelRegion.getRegionWidth();
        float rightX = Constants.UNIT_FOR_PIXEL * carRegion.getRegionWidth() / 2 - wheelW / 2 + 0.05f;
        float leftX = -rightX;
        float rearY = Constants.UNIT_FOR_PIXEL * -carRegion.getRegionHeight() / 2 + REAR_WHEEL_Y;
        float frontY = rearY + WHEEL_BASE + 0.2f;

        Vehicle.WheelInfo info;
        info = mPlayerVehicle.addWheel(wheelRegion, leftX, frontY);
        info.steeringFactor = 1;
        info = mPlayerVehicle.addWheel(wheelRegion, rightX, frontY);
        info.steeringFactor = 1;
        info = mPlayerVehicle.addWheel(wheelRegion, leftX, rearY);
        //info.wheel.setCanDrift(true);
        info = mPlayerVehicle.addWheel(wheelRegion, rightX, rearY);
        //info.wheel.setCanDrift(true);

        addGameObject(mPlayerVehicle);
    }

    private void setupOutsideWalls() {
        float mapWidth = mMapInfo.getMapWidth();
        float mapHeight = mMapInfo.getMapHeight();
        float wallSize = 1;
        // bottom
        Box2DUtils.createStaticBox(mBox2DWorld, 0, -wallSize, mapWidth, wallSize);
        // top
        Box2DUtils.createStaticBox(mBox2DWorld, 0, mapHeight, mapWidth, wallSize);
        // left
        Box2DUtils.createStaticBox(mBox2DWorld, -wallSize, 0, wallSize, mapHeight);
        // right
        Box2DUtils.createStaticBox(mBox2DWorld, mapWidth, 0, wallSize, mapHeight);
    }

    private void setupObjects() {
        MapLayer obstacleLayer = mMapInfo.getObstaclesLayer();
        TiledMapTileLayer wallsLayer = mMapInfo.getWallsLayer();
        ObstacleCreator creator = new ObstacleCreator(this, mGame.getAssets(), mMapInfo.getMap().getTileSets(), wallsLayer);
        for (MapObject object : obstacleLayer.getObjects()) {
            creator.create(object);
        }
    }

    public void addSkidmarkAt(Vector2 position) {
        Vector2 pos = mSkidmarks[mSkidmarksIndex];
        if (pos == null) {
            pos = new Vector2();
            mSkidmarks[mSkidmarksIndex] = pos;
        }
        pos.x = position.x;
        pos.y = position.y;
        mSkidmarksIndex = (mSkidmarksIndex + 1) % mSkidmarks.length;
    }

    @Override
    public void beginContact(Contact contact) {
        Object userA = contact.getFixtureA().getBody().getUserData();
        Object userB = contact.getFixtureB().getBody().getUserData();
        if (userA instanceof Collidable) {
            ((Collidable) userA).beginContact(contact, contact.getFixtureB());
        }
        if (userB instanceof Collidable) {
            ((Collidable) userB).beginContact(contact, contact.getFixtureA());
        }
    }

    @Override
    public void endContact(Contact contact) {
        Object userA = contact.getFixtureA().getBody().getUserData();
        Object userB = contact.getFixtureB().getBody().getUserData();
        if (userA instanceof Collidable) {
            ((Collidable) userA).endContact(contact, contact.getFixtureB());
        }
        if (userB instanceof Collidable) {
            ((Collidable) userB).endContact(contact, contact.getFixtureA());
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Object userA = contact.getFixtureA().getBody().getUserData();
        Object userB = contact.getFixtureB().getBody().getUserData();
        if (userA instanceof Collidable) {
            ((Collidable) userA).preSolve(contact, contact.getFixtureB(), oldManifold);
        }
        if (userB instanceof Collidable) {
            ((Collidable) userB).preSolve(contact, contact.getFixtureA(), oldManifold);
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        Object userA = contact.getFixtureA().getBody().getUserData();
        Object userB = contact.getFixtureB().getBody().getUserData();
        if (userA instanceof Collidable) {
            ((Collidable) userA).postSolve(contact, contact.getFixtureB(), impulse);
        }
        if (userB instanceof Collidable) {
            ((Collidable) userB).postSolve(contact, contact.getFixtureA(), impulse);
        }
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
    }

    @Override
    public void dispose() {
        mMapInfo.dispose();
    }

}
