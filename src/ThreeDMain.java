import com.jme3.app.SimpleApplication;
import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

public class ThreeDMain extends SimpleApplication {
    private static final float WALK_SPEED = 6f;
    private static final float SPRINT_SPEED = 10f;
    private static final float WORLD_LIMIT = 500f;
    private static final float INTERACTION_DISTANCE = 3.5f;
    private static final float PLAYER_COLLIDER_RADIUS = 0.42f;
    private static final float MONSTER_CONTACT_DISTANCE = 1.15f;
    private static final float AI_ATTACK_RANGE = 1.85f;
    private static final float PLAYER_THREAT_RANGE = 12f;
    private static final float HOT_ZONE_PLAYER_THREAT_RANGE = 20f;
    private static final float FACTION_BASE_RADIUS = 7f;
    private static final float PLAYER_RESPAWN_SECONDS = 5f;
    private static final float PLAYER_RESPAWN_PROTECTION_SECONDS = 3f;
    private static final float ARCHER_PROJECTILE_SPEED = 19f;
    private static final int MERCHANT_PRICE = 3;
    private static final int MAP_EVENT_COUNT = 12;
    private static final int TOTAL_BATTLE_AI = 100;
    private static final int TRANSPORTS_PER_FACTION = 3;
    private static final int AI_RESPAWN_SECONDS = 5;
    private static final int HILL_SCORE_INTERVAL_SECONDS = 30;
    private static final int HILL_TARGET_SCORE = 100;
    private static final float HOT_ZONE_TARGET_INTERVAL_SECONDS = 60f;
    private static final float HOT_ZONE_MOVE_SPEED = 0.65f;
    private static final float CONTROL_AREA_RADIUS = 24f;
    private static final float FOB_BUILD_SECONDS = 12f;
    private static final float FOB_RESPAWN_SECONDS = 2f;
    private static final int TRANSPORT_CAPACITY = 5;
    private static final float TRANSPORT_SPEED = WALK_SPEED * 1.5f;
    private static final float TRANSPORT_BOARDING_SECONDS = 6f;
    private static final String[] HILL_ZONE_NAMES = {"北部高地", "中央山丘", "南部山丘"};
    private static final String UI_FONT = "Microsoft YaHei UI";

    private final Vector3f movement = new Vector3f();
    private final boolean[] keysDown = new boolean[6];
    private final Random random = new Random();
    private final Player adventurer = new Player();
    private final GameStateManager gameStates = new GameStateManager();
    private final List<String> messages = new ArrayList<>();
    private final List<MapEvent> mapEvents = new ArrayList<>();
    private final List<WorldObstacle> worldObstacles = new ArrayList<>();
    private final List<MenuButton> menuButtons = new ArrayList<>();
    private final List<BattleActor> allies = new ArrayList<>();
    private final List<BattleActor> enemies = new ArrayList<>();
    private final List<BattleActor> greenForces = new ArrayList<>();
    private final EnumMap<Faction, Integer> factionScores = new EnumMap<>(Faction.class);
    private final EnumMap<Faction, Vector3f> factionBases = new EnumMap<>(Faction.class);
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<BattleEffect> battleEffects = new ArrayList<>();
    private final List<DefenseStructure> defenseStructures = new ArrayList<>();
    private final List<Decoy> rogueDecoys = new ArrayList<>();
    private final List<Vector3f> highGroundPositions = new ArrayList<>();
    private final EnumMap<Faction, ForwardBase> forwardBases = new EnumMap<>(Faction.class);
    private final List<TransportWagon> transportWagons = new ArrayList<>();
    private Node player;
    private Node torsoPivot;
    private Node headPivot;
    private Node leftArmPivot;
    private Node rightArmPivot;
    private Node leftLegPivot;
    private Node rightLegPivot;
    private Node monsterModel;
    private Node hotZoneNode;
    private Node controlAreaNode;
    private Geometry hotZoneDisc;
    private Material arrowShaftMaterial;
    private Material arrowTipMaterial;
    private Monster currentMonster;
    private MapEvent nearbyEvent;
    private AnimComposer playerAnimator;
    private LocomotionState locomotionState;
    private Vector3f facingDirection = new Vector3f(0f, 0f, -1f);
    private String idleAnimation;
    private String runAnimation;
    private boolean sprinting;
    private boolean merchantAvailable;
    private boolean proceduralCharacter;
    private GameAudio gameAudio;
    private String audioStatus = "音效初始化中";
    private int score;
    private int defeatedMonsters;
    private float animationTime;
    private float attackAnimationTime;
    private float monsterContactTimer;
    private float playerHitFeedbackTimer;
    private final Vector3f playerKnockback = new Vector3f();
    private float playerStunTimer;
    private float playerRespawnTimer;
    private float playerSpawnProtectionTimer;
    private float hillScoreTimer;
    private float hotZoneRetargetTimer;
    private Vector3f hotZoneTarget = new Vector3f();
    private int hillZoneIndex = 1;
    private String currentHotZoneLabel = "中央山丘";
    private Faction hillLeader;
    private float hudRefreshTimer;
    private Geometry hudGeometry;
    private Texture2D hudTexture;
    private String promptMessage = "尋找地圖上的彩色探索標記，靠近後按 E";
    private ColorRGBA promptColor = ColorRGBA.White;

    public static void main(String[] args) {
        ThreeDMain app = new ThreeDMain();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("冒險者之旅 - 3D 原型");
        settings.setResolution(1280, 800);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        setDisplayStatView(false);
        setDisplayFps(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.52f, 0.72f, 0.88f, 1f));

        addLighting();
        addGround();
        initializeFactionBases();
        addScenery();
        createPlayer();
        createHillControlArea();
        createHighGrounds();
        createFactionBases();
        createBattleSquad();
        initializeForwardBases();
        createTransportWagons();
        for (Faction faction : Faction.values()) {
            factionScores.put(faction, 0);
        }
        populateMapEvents();
        configureInput();
        inputManager.setCursorVisible(true);
        createHud();
        initializeAudio();
        updateHud();
        log("歡迎來到冒險者之旅。");
        updateCamera();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!gameStates.isPlaying()) {
            return;
        }
        animationTime += tpf;
        attackAnimationTime = Math.max(0f, attackAnimationTime - tpf);
        playerHitFeedbackTimer = Math.max(0f, playerHitFeedbackTimer - tpf);
        playerSpawnProtectionTimer = Math.max(0f, playerSpawnProtectionTimer - tpf);
        hudRefreshTimer -= tpf;
        hillScoreTimer += tpf;
        hotZoneRetargetTimer += tpf;
        movement.set(
                (keysDown[1] ? 1f : 0f) - (keysDown[0] ? 1f : 0f),
                0f,
                (keysDown[3] ? 1f : 0f) - (keysDown[2] ? 1f : 0f));

        if (!adventurer.isAlive()) {
            playerRespawnTimer -= tpf;
            if (playerRespawnTimer <= 0f) {
                respawnPlayer();
            }
        } else {
            updateKnockback(player, playerKnockback, tpf, true, Faction.BLUE);
        }
        if (adventurer.isAlive() && playerStunTimer <= 0f && movement.lengthSquared() > 0f) {
            movement.normalizeLocal();
            float speed = sprinting ? SPRINT_SPEED : WALK_SPEED;
            Vector3f nextPosition = player.getLocalTranslation().add(movement.mult(speed * tpf));
            nextPosition.x = FastMath.clamp(nextPosition.x, -WORLD_LIMIT, WORLD_LIMIT);
            nextPosition.z = FastMath.clamp(nextPosition.z, -WORLD_LIMIT, WORLD_LIMIT);
            if (canMoveTo(nextPosition)) {
                player.setLocalTranslation(nextPosition);
            } else {
                Vector3f currentPosition = player.getLocalTranslation();
                Vector3f slideX = new Vector3f(nextPosition.x, currentPosition.y,
                        currentPosition.z);
                Vector3f slideZ = new Vector3f(currentPosition.x, currentPosition.y,
                        nextPosition.z);
                if (canMoveTo(slideX)) {
                    player.setLocalTranslation(slideX);
                }
                if (canMoveTo(slideZ)) {
                    player.setLocalTranslation(slideZ);
                }
            }
            facingDirection.set(movement);
            faceActor(player, player.getLocalTranslation().add(facingDirection));
        }
        updateMonsterContact(tpf);
        updateForwardBases(tpf);
        updateTransportWagons(tpf);
        updateBattleActors(tpf);
        updateProjectiles(tpf);
        updateDefenseStructures(tpf);
        updateBattleEffects(tpf);
        updateRogueDecoys(tpf);
        updateHotZone(tpf);
        if (hillScoreTimer >= HILL_SCORE_INTERVAL_SECONDS) {
            hillScoreTimer -= HILL_SCORE_INTERVAL_SECONDS;
            scoreHillControl();
        }
        updateLocomotionAnimation();
        updateProceduralAnimation();
        updateNearbyEvent();
        updateCamera();
        if (hudRefreshTimer <= 0f) {
            hudRefreshTimer = 0.2f;
            renderHud();
        }
    }

    @Override
    public void simpleRender(RenderManager renderManager) {
    }

    @Override
    public void destroy() {
        if (gameAudio != null) {
            gameAudio.close();
        }
        super.destroy();
    }

    private void initializeAudio() {
        try {
            gameAudio = new GameAudio();
            audioStatus = "背景音樂與音效已啟用";
        } catch (LineUnavailableException exception) {
            audioStatus = "音效裝置無法使用：" + exception.getMessage();
            System.err.println(audioStatus);
        } catch (IllegalStateException exception) {
            audioStatus = "音效初始化失敗：" + exception.getMessage();
            System.err.println(audioStatus);
        }
        updateHud();
    }

    private void playAttackSound() {
        if (gameAudio != null) {
            gameAudio.playAttack();
        }
    }

    private void playHitSound() {
        if (gameAudio != null) {
            gameAudio.playHit();
        }
    }

    private void playHealSound() {
        if (gameAudio != null) {
            gameAudio.playHeal();
        }
    }

    private void playRewardSound() {
        if (gameAudio != null) {
            gameAudio.playReward();
        }
    }

    private void addLighting() {
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.72f, 0.72f, 0.72f, 1f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4f, -1f, -0.35f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.15f));
        rootNode.addLight(sun);
    }

    private void addGround() {
        Geometry ground = new Geometry("1000x1000 tactical battlefield", new Box(500f, 0.2f, 500f));
        ground.setMaterial(litMaterial(new ColorRGBA(0.28f, 0.48f, 0.23f, 1f)));
        ground.setLocalTranslation(0f, -0.2f, 0f);
        rootNode.attachChild(ground);

        Material pathMaterial = litMaterial(new ColorRGBA(0.62f, 0.53f, 0.36f, 1f));
        Geometry path = new Geometry("Dirt path", new Box(3.5f, 0.015f, 500f));
        path.setMaterial(pathMaterial);
        path.setLocalTranslation(0f, 0.005f, 0f);
        rootNode.attachChild(path);
    }

    private void addScenery() {
        Random random = new Random(2026L);
        Material trunkMaterial = litMaterial(new ColorRGBA(0.32f, 0.19f, 0.1f, 1f));
        Material leavesMaterial = litMaterial(new ColorRGBA(0.12f, 0.34f, 0.15f, 1f));
        Material rockMaterial = litMaterial(new ColorRGBA(0.38f, 0.4f, 0.4f, 1f));

        for (int i = 0; i < 650; i++) {
            float x = random.nextFloat() * 960f - 480f;
            float z = random.nextFloat() * 960f - 480f;
            if (FastMath.abs(x) < 5f && FastMath.abs(z) < 7f) {
                continue;
            }

            Node tree = new Node("Tree");
            Geometry trunk = new Geometry("Trunk", new Cylinder(8, 10, 0.28f, 2.2f));
            trunk.setMaterial(trunkMaterial);
            trunk.setLocalTranslation(0f, 1.1f, 0f);
            tree.attachChild(trunk);

            Geometry leaves = new Geometry("Canopy", new Sphere(12, 12, 1f));
            leaves.setMaterial(leavesMaterial);
            leaves.setLocalScale(1.1f, 1.45f, 1.1f);
            leaves.setLocalTranslation(0f, 2.65f, 0f);
            tree.attachChild(leaves);
            tree.setLocalTranslation(x, 0f, z);
            rootNode.attachChild(tree);
        }

        for (int i = 0; i < 240; i++) {
            Geometry rock = new Geometry("Rock", new Sphere(8, 8, 1f));
            rock.setMaterial(rockMaterial);
            float scaleX = 0.35f + random.nextFloat() * 0.55f;
            float scaleY = 0.25f + random.nextFloat() * 0.35f;
            float scaleZ = 0.3f + random.nextFloat() * 0.5f;
            float x = random.nextFloat() * 960f - 480f;
            float z = random.nextFloat() * 960f - 480f;
            Vector3f rockPosition = new Vector3f(x, 0f, z);
            boolean nearBaseOrFob = false;
            for (Faction faction : Faction.values()) {
                Vector3f base = factionBases.get(faction);
                Vector3f fobSite = new Vector3f(base.x * 0.42f, 0f, base.z * 0.42f);
                if (distance2D(rockPosition, base) < FACTION_BASE_RADIUS + 2f
                        || distance2D(rockPosition, fobSite) < 4f) {
                    nearBaseOrFob = true;
                    break;
                }
            }
            if (x * x + z * z < 9f || nearBaseOrFob) {
                continue;
            }
            rock.setLocalScale(scaleX, scaleY, scaleZ);
            rock.setLocalTranslation(x, 0.15f, z);
            worldObstacles.add(new WorldObstacle(x, z, 0.15f,
                    Math.max(scaleX, scaleZ), scaleY * 2f));
            rootNode.attachChild(rock);
        }
    }

    private void createPlayer() {
        player = new Node("Player");
        try {
            Spatial characterModel = assetManager.loadModel("Models/player.glb");
            player.attachChild(characterModel);
            playerAnimator = findAnimator(characterModel);
            if (playerAnimator != null) {
                configureCharacterAnimations();
            } else {
                log("角色模型沒有找到骨架動畫；請確認 GLB 匯出時有包含動畫。");
            }
            System.out.println("Loaded optional character model: Models/player.glb");
        } catch (AssetNotFoundException exception) {
            System.out.println("Optional Models/player.glb not found; using the built-in 3D adventurer.");
            createFallbackCharacter();
        }
        player.setLocalTranslation(factionBases.get(Faction.BLUE));
        player.setCullHint(Spatial.CullHint.Inherit);
        rootNode.attachChild(player);
    }

    private void createHillControlArea() {
        controlAreaNode = new Node("King of the Hill control area");
        Material boundaryMaterial = unshadedMaterial(new ColorRGBA(1f, 0.82f, 0.22f, 0.9f));
        for (int i = 0; i < 64; i++) {
            float angle = FastMath.TWO_PI * i / 64f;
            Geometry boundaryMarker = new Geometry("Control area boundary",
                    new Cylinder(8, 8, 0.24f, 0.12f));
            boundaryMarker.setMaterial(boundaryMaterial);
            boundaryMarker.setLocalTranslation(FastMath.cos(angle) * CONTROL_AREA_RADIUS,
                    0.04f, FastMath.sin(angle) * CONTROL_AREA_RADIUS);
            controlAreaNode.attachChild(boundaryMarker);
        }
        rootNode.attachChild(controlAreaNode);

        hotZoneNode = new Node("Moving hill hot zone");
        hotZoneDisc = new Geometry("Hot zone ground marker",
                new Cylinder(40, 40, 5.2f, 0.09f, true));
        Material hotMaterial = unshadedMaterial(new ColorRGBA(1f, 0.57f, 0.12f, 0.55f));
        hotMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        hotMaterial.getAdditionalRenderState().setDepthWrite(false);
        hotZoneDisc.setMaterial(hotMaterial);
        hotZoneDisc.setLocalTranslation(0f, 0.14f, 0f);
        hotZoneNode.attachChild(hotZoneDisc);

        Geometry beacon = new Geometry("Hot zone beacon", new Cylinder(12, 12, 0.24f, 3.4f));
        beacon.setMaterial(unshadedMaterial(new ColorRGBA(1f, 0.65f, 0.1f, 0.95f)));
        beacon.setLocalTranslation(0f, 1.8f, 0f);
        hotZoneNode.attachChild(beacon);
        for (int i = 0; i < 4; i++) {
            float angle = FastMath.HALF_PI * i;
            Geometry pillar = new Geometry("Hot zone ring light",
                    new Sphere(10, 10, 0.42f));
            pillar.setMaterial(unshadedMaterial(new ColorRGBA(1f, 0.86f, 0.23f, 1f)));
            pillar.setLocalTranslation(FastMath.cos(angle) * 5.1f, 0.65f,
                    FastMath.sin(angle) * 5.1f);
            hotZoneNode.attachChild(pillar);
        }
        hotZoneNode.setLocalTranslation(0f, 0f, 0f);
        rootNode.attachChild(hotZoneNode);
        chooseNextHotZoneTarget();
    }

    private void initializeFactionBases() {
        factionBases.put(Faction.RED, new Vector3f(-420f, 0f, -400f));
        factionBases.put(Faction.GREEN, new Vector3f(0f, 0f, 430f));
        factionBases.put(Faction.BLUE, new Vector3f(420f, 0f, -400f));
    }

    private void createHighGrounds() {
        float[][] locations = {
            {-180f, -120f}, {180f, -120f}, {-180f, 120f}, {180f, 120f}
        };
        Material hillMaterial = litMaterial(new ColorRGBA(0.42f, 0.49f, 0.31f, 1f));
        Material markerMaterial = unshadedMaterial(new ColorRGBA(0.96f, 0.82f, 0.34f, 0.95f));
        for (int i = 0; i < locations.length; i++) {
            Vector3f position = new Vector3f(locations[i][0], 0f, locations[i][1]);
            highGroundPositions.add(position);
            Node hill = new Node("Tactical high ground " + (i + 1));
            Geometry mound = new Geometry("Raised high ground", new Sphere(24, 16, 1f));
            mound.setMaterial(hillMaterial);
            mound.setLocalScale(7f, 0.8f, 6f);
            mound.setLocalTranslation(0f, -0.35f, 0f);
            hill.attachChild(mound);
            Geometry marker = new Geometry("High ground beacon", new Cylinder(8, 8, 0.16f, 2.4f));
            marker.setMaterial(markerMaterial);
            marker.setLocalTranslation(0f, 1.45f, 0f);
            hill.attachChild(marker);
            hill.setLocalTranslation(position);
            rootNode.attachChild(hill);
        }
    }

    private void initializeForwardBases() {
        for (Faction faction : Faction.values()) {
            Vector3f home = factionBases.get(faction);
            Vector3f objective = new Vector3f(home.x * 0.42f, 0f, home.z * 0.42f);
            forwardBases.put(faction, new ForwardBase(faction, objective));
        }
    }

    private void updateForwardBases(float tpf) {
        for (ForwardBase forwardBase : forwardBases.values()) {
            if (forwardBase.built) {
                continue;
            }
            if (forwardBase.builder == null || !forwardBase.builder.alive
                    || forwardBase.builder.ridingWagon) {
                forwardBase.builder = closestForwardBaseBuilder(forwardBase);
                forwardBase.building = false;
                if (forwardBase.marker != null) {
                    forwardBase.marker.removeFromParent();
                    forwardBase.marker = null;
                }
            }
            if (forwardBase.builder == null) {
                continue;
            }
            if (!forwardBase.building
                    && distance2D(forwardBase.builder.model.getLocalTranslation(),
                            forwardBase.position) <= 2.2f) {
                forwardBase.building = true;
                forwardBase.marker = createForwardBaseConstructionMarker(forwardBase);
                log(forwardBase.builder.name + " 開始建造" + forwardBase.faction.getDisplayName()
                        + " 前線基地 FOB，預計 " + (int) Math.ceil(forwardBase.buildRemaining)
                        + " 秒完成。");
            }
            if (forwardBase.building) {
                forwardBase.buildRemaining = Math.max(0f, forwardBase.buildRemaining - tpf);
                if (forwardBase.buildRemaining <= 0f) {
                    completeForwardBase(forwardBase);
                }
            }
        }
    }

    private BattleActor closestForwardBaseBuilder(ForwardBase forwardBase) {
        BattleActor closestEngineer = null;
        BattleActor closestLogistics = null;
        float engineerDistance = Float.MAX_VALUE;
        float logisticsDistance = Float.MAX_VALUE;
        for (BattleActor actor : allBattleActors()) {
            if (!actor.alive || actor.ridingWagon || actor.faction != forwardBase.faction
                    || (actor.job != JobType.ENGINEER && actor.job != JobType.LOGISTICS)) {
                continue;
            }
            float distance = distance2D(actor.model.getLocalTranslation(), forwardBase.position);
            if (actor.job == JobType.ENGINEER && distance < engineerDistance) {
                closestEngineer = actor;
                engineerDistance = distance;
            } else if (actor.job == JobType.LOGISTICS && distance < logisticsDistance) {
                closestLogistics = actor;
                logisticsDistance = distance;
            }
        }
        return closestEngineer != null ? closestEngineer : closestLogistics;
    }

    private Node createForwardBaseConstructionMarker(ForwardBase forwardBase) {
        Node marker = new Node("FOB construction site");
        Geometry frame = new Geometry("FOB construction frame", new Box(1.8f, 1.2f, 1.8f));
        Material material = unshadedMaterial(new ColorRGBA(1f, 0.82f, 0.22f, 0.62f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setWireframe(true);
        frame.setMaterial(material);
        frame.setLocalTranslation(0f, 1.2f, 0f);
        marker.attachChild(frame);
        marker.setLocalTranslation(forwardBase.position);
        rootNode.attachChild(marker);
        return marker;
    }

    private void completeForwardBase(ForwardBase forwardBase) {
        if (forwardBase.marker != null) {
            forwardBase.marker.removeFromParent();
            forwardBase.marker = null;
        }
        Node model = new Node(forwardBase.faction.getDisplayName() + " FOB");
        Geometry pad = new Geometry("FOB landing pad", new Cylinder(2, 24, 3.2f, 0.24f));
        pad.setMaterial(litMaterial(factionColor(forwardBase.faction)));
        pad.setLocalTranslation(0f, 0.12f, 0f);
        model.attachChild(pad);
        Geometry shelter = new Geometry("FOB shelter", new Box(1.4f, 1f, 1.2f));
        shelter.setMaterial(litMaterial(new ColorRGBA(0.38f, 0.39f, 0.34f, 1f)));
        shelter.setLocalTranslation(0f, 1.15f, 0f);
        model.attachChild(shelter);
        Geometry beacon = new Geometry("FOB beacon", new Cylinder(10, 10, 0.22f, 3.2f));
        beacon.setMaterial(unshadedMaterial(factionColor(forwardBase.faction)));
        beacon.setLocalTranslation(0f, 3.25f, 0f);
        model.attachChild(beacon);
        model.setLocalTranslation(forwardBase.position);
        rootNode.attachChild(model);
        forwardBase.model = model;
        forwardBase.built = true;
        forwardBase.building = false;
        log(forwardBase.faction.getDisplayName()
                + " 前線基地 FOB 建成！陣亡單位將在 2 秒後於 FOB 重生。");
    }

    private void createTransportWagons() {
        transportWagons.clear();
        for (Faction faction : Faction.values()) {
            List<BattleActor> drivers = new ArrayList<>();
            for (BattleActor actor : allBattleActors()) {
                if (actor.faction == faction && actor.job == JobType.LOGISTICS) {
                    drivers.add(actor);
                }
            }
            int wagonCount = Math.min(TRANSPORTS_PER_FACTION, drivers.size());
            if (wagonCount == 0) {
                continue;
            }
            for (int wagonIndex = 0; wagonIndex < wagonCount; wagonIndex++) {
                int driverIndex = wagonIndex * drivers.size() / wagonCount;
                BattleActor driver = drivers.get(driverIndex);
                Vector3f home = factionBases.get(faction);
                Vector3f inward = new Vector3f(-home.x, 0f, -home.z).normalizeLocal();
                Vector3f lateral = new Vector3f(-inward.z, 0f, inward.x);
                float laneOffset = (wagonIndex - (wagonCount - 1) * 0.5f) * 5f;
                Vector3f pickup = home.add(inward.mult(9f))
                        .addLocal(lateral.mult(laneOffset));
                Node model = createTransportWagonModel(faction);
                model.setLocalTranslation(pickup);
                rootNode.attachChild(model);
                TransportWagon wagon = new TransportWagon(faction, driver, model, pickup,
                        wagonIndex);
                transportWagons.add(wagon);
                driver.transportWagon = wagon;
                driver.ridingWagon = true;
                driver.model.setLocalTranslation(pickup.add(-1.2f, 0f, 0f));
                wagon.passengers.add(driver);
                chooseWagonPassengers(wagon);
                log(faction.getDisplayName() + " 運輸兵已備妥馬車，乘員上限 "
                        + TRANSPORT_CAPACITY + " 人。");
            }
        }
    }

    private Node createTransportWagonModel(Faction faction) {
        Node wagon = new Node(faction.getDisplayName() + " transport wagon");
        Material wood = litMaterial(new ColorRGBA(0.42f, 0.24f, 0.12f, 1f));
        Material team = litMaterial(factionColor(faction));
        Geometry bed = new Geometry("Wagon bed", new Box(1.5f, 0.28f, 1.9f));
        bed.setMaterial(wood);
        bed.setLocalTranslation(0f, 1.15f, 0f);
        wagon.attachChild(bed);
        Geometry cargo = new Geometry("Wagon cargo", new Box(0.9f, 0.55f, 0.8f));
        cargo.setMaterial(team);
        cargo.setLocalTranslation(0f, 1.9f, -0.25f);
        wagon.attachChild(cargo);
        for (int side : new int[] {-1, 1}) {
            Geometry wheel = new Geometry("Wagon wheel", new Cylinder(2, 12, 0.78f, 0.22f));
            wheel.setMaterial(wood);
            wheel.setLocalRotation(new Quaternion().fromAngleAxis(
                    FastMath.HALF_PI, Vector3f.UNIT_X));
            wheel.setLocalTranslation(side * 1.65f, 0.8f, -0.1f);
            wagon.attachChild(wheel);
        }
        Geometry horseBody = new Geometry("Draft horse", new Box(0.62f, 0.7f, 1.05f));
        horseBody.setMaterial(new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"));
        horseBody.getMaterial().setBoolean("UseMaterialColors", true);
        horseBody.getMaterial().setColor("Diffuse", new ColorRGBA(0.48f, 0.3f, 0.17f, 1f));
        horseBody.getMaterial().setColor("Ambient", new ColorRGBA(0.3f, 0.18f, 0.1f, 1f));
        horseBody.setLocalTranslation(0f, 1.05f, 2.1f);
        wagon.attachChild(horseBody);
        Geometry horseHead = new Geometry("Draft horse head", new Box(0.38f, 0.58f, 0.42f));
        horseHead.setMaterial(horseBody.getMaterial());
        horseHead.setLocalTranslation(0f, 1.65f, 2.85f);
        wagon.attachChild(horseHead);
        Geometry teamFlag = new Geometry("Transport faction flag", new Box(0.08f, 0.65f, 0.45f));
        teamFlag.setMaterial(unshadedMaterial(factionColor(faction)));
        teamFlag.setLocalTranslation(0f, 2.55f, -1.45f);
        wagon.attachChild(teamFlag);
        return wagon;
    }

    private void chooseWagonPassengers(TransportWagon wagon) {
        wagon.boardingCandidates.clear();
        wagon.boardingCandidates.add(wagon.driver);
        List<BattleActor> candidates = new ArrayList<>();
        for (BattleActor actor : allBattleActors()) {
            if (actor.alive && actor.faction == wagon.faction && actor != wagon.driver
                    && actor.transportWagon == null && actor.job != JobType.ENGINEER
                    && actor.job != JobType.LOGISTICS
                    && distance2D(actor.model.getLocalTranslation(), wagon.pickup) <= 26f) {
                candidates.add(actor);
            }
        }
        Collections.sort(candidates, (first, second) -> Float.compare(
                distance2D(first.model.getLocalTranslation(), wagon.pickup),
                distance2D(second.model.getLocalTranslation(), wagon.pickup)));
        int passengerSlots = TRANSPORT_CAPACITY - 1;
        for (int i = 0; i < candidates.size() && i < passengerSlots; i++) {
            BattleActor candidate = candidates.get(i);
            candidate.transportWagon = wagon;
            wagon.boardingCandidates.add(candidate);
        }
        wagon.boardingTimer = TRANSPORT_BOARDING_SECONDS;
        wagon.phase = wagon.boardingCandidates.size() > 1
                ? TransportPhase.BOARDING : TransportPhase.IDLE;
        wagon.idleTimer = 8f;
        wagon.boardingAnnouncementPending = wagon.phase == TransportPhase.BOARDING;
    }

    private void updateTransportWagons(float tpf) {
        for (TransportWagon wagon : transportWagons) {
            if (wagon.phase == TransportPhase.BOARDING) {
                if (wagon.boardingAnnouncementPending) {
                    transportAnnouncement("[運輸兵喊話]：上車！我們要前往熱區！");
                    wagon.boardingAnnouncementPending = false;
                }
                for (BattleActor candidate : wagon.boardingCandidates) {
                    if (!candidate.alive || candidate == wagon.driver
                            || candidate.ridingWagon) {
                        if (!candidate.alive && candidate.transportWagon == wagon) {
                            candidate.transportWagon = null;
                        }
                        continue;
                    }
                    if (distance2D(candidate.model.getLocalTranslation(),
                            wagon.position) <= 2.8f) {
                        candidate.ridingWagon = true;
                        candidate.transportWagon = wagon;
                        candidate.model.setCullHint(Spatial.CullHint.Always);
                        candidate.model.setLocalTranslation(wagon.position);
                        wagon.passengers.add(candidate);
                        log(candidate.name + " 已登上" + wagon.faction.getDisplayName()
                                + " 運輸馬車。");
                    }
                }
                wagon.boardingTimer -= tpf;
                if (wagon.boardingTimer <= 0f) {
                    for (BattleActor candidate : wagon.boardingCandidates) {
                        if (candidate != wagon.driver && !candidate.ridingWagon
                                && candidate.transportWagon == wagon) {
                            candidate.transportWagon = null;
                        }
                    }
                    wagon.boardingCandidates.clear();
                    if (wagon.passengers.size() > 1) {
                        wagon.phase = TransportPhase.OUTBOUND;
                        wagon.destinationMode = chooseTransportDestination(wagon);
                        wagon.destination = transportDropoff(wagon, wagon.destinationMode);
                        transportAnnouncement("[運輸兵喊話]：" + wagon.driver.name
                                + " 馬車出發，前往" + wagon.destinationMode.label + "！車上 "
                                + wagon.passengers.size()
                                + "/" + TRANSPORT_CAPACITY + " 人。");
                    } else {
                        wagon.phase = TransportPhase.IDLE;
                        wagon.destination = null;
                        wagon.idleTimer = 8f;
                    }
                }
            } else if (wagon.phase == TransportPhase.OUTBOUND) {
                if (moveWagon(wagon, wagon.destination, tpf)) {
                    unloadWagon(wagon);
                }
            } else if (wagon.phase == TransportPhase.RETURNING) {
                if (moveWagon(wagon, wagon.pickup, tpf)) {
                    wagon.passengers.clear();
                    wagon.passengers.add(wagon.driver);
                    wagon.driver.ridingWagon = true;
                    wagon.driver.model.setCullHint(Spatial.CullHint.Inherit);
                    chooseWagonPassengers(wagon);
                    if (wagon.phase == TransportPhase.BOARDING) {
                        transportAnnouncement("[運輸兵喊話]：" + wagon.faction.getDisplayName()
                                + " 馬車返回集合點，上車準備再次出發！");
                    }
                }
            } else if (wagon.phase == TransportPhase.IDLE) {
                wagon.idleTimer -= tpf;
                if (wagon.idleTimer <= 0f) {
                    chooseWagonPassengers(wagon);
                }
            }
            wagon.driver.model.setLocalTranslation(wagon.position.add(-1.2f, 0f, 0f));
            faceActor(wagon.model, wagon.destination == null
                    ? wagon.position.add(0f, 0f, 1f) : wagon.destination);
        }
    }

    private boolean moveWagon(TransportWagon wagon, Vector3f destination, float tpf) {
        Vector3f delta = destination.subtract(wagon.position);
        float distance = delta.length();
        if (distance <= 1.5f) {
            wagon.position.set(destination);
            wagon.model.setLocalTranslation(wagon.position);
            return true;
        }
        float step = Math.min(distance, TRANSPORT_SPEED * tpf);
        wagon.position.addLocal(delta.normalizeLocal().multLocal(step));
        wagon.position.x = FastMath.clamp(wagon.position.x, -WORLD_LIMIT, WORLD_LIMIT);
        wagon.position.z = FastMath.clamp(wagon.position.z, -WORLD_LIMIT, WORLD_LIMIT);
        wagon.model.setLocalTranslation(wagon.position);
        return false;
    }

    private Vector3f transportDropoff(TransportWagon wagon,
                                      TransportDestination destinationMode) {
        if (destinationMode == TransportDestination.FORWARD_BASE) {
            ForwardBase forwardBase = forwardBases.get(wagon.faction);
            return forwardBase.position.clone();
        }
        if (destinationMode == TransportDestination.ENEMY_REAR) {
            return nearestEnemyRearPosition(wagon);
        }
        Vector3f hotZone = hotZoneNode.getLocalTranslation();
        Vector3f offset = wagon.pickup.subtract(hotZone);
        if (offset.lengthSquared() < 0.01f) {
            offset.set(1f, 0f, 0f);
        }
        Vector3f dropoff = hotZone.add(offset.normalizeLocal().multLocal(12f));
        dropoff.x = FastMath.clamp(dropoff.x, -WORLD_LIMIT + 5f, WORLD_LIMIT - 5f);
        dropoff.z = FastMath.clamp(dropoff.z, -WORLD_LIMIT + 5f, WORLD_LIMIT - 5f);
        return dropoff;
    }

    private Vector3f nearestEnemyRearPosition(TransportWagon wagon) {
        Vector3f nearestBase = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Faction faction : Faction.values()) {
            if (faction == wagon.faction) {
                continue;
            }
            Vector3f base = factionBases.get(faction);
            float distance = distance2D(wagon.position, base);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestBase = base;
            }
        }
        Vector3f inward = new Vector3f(-nearestBase.x, 0f, -nearestBase.z);
        if (inward.lengthSquared() < 0.01f) {
            inward.set(1f, 0f, 0f);
        }
        inward.normalizeLocal();
        return nearestBase.add(inward.mult(FACTION_BASE_RADIUS + 6f));
    }

    private TransportDestination chooseTransportDestination(TransportWagon wagon) {
        Vector3f hotZone = hotZoneNode.getLocalTranslation();
        Vector3f fob = forwardBases.get(wagon.faction).position;
        Vector3f enemyRear = nearestEnemyRearPosition(wagon);
        int friendliesAtHotZone = countFactionNear(wagon.faction, hotZone, 35f);
        int enemiesAtHotZone = countEnemiesNear(wagon.faction, hotZone, 35f);
        int friendliesAtFob = countFactionNear(wagon.faction, fob, 30f);
        int enemiesAtFob = countEnemiesNear(wagon.faction, fob, 30f);
        int friendliesAtRear = countFactionNear(wagon.faction, enemyRear, 30f);
        int enemiesAtRear = countEnemiesNear(wagon.faction, enemyRear, 30f);
        boolean fobBuilt = forwardBases.get(wagon.faction).built;

        float[] scores = {
            10f + enemiesAtHotZone * 1.1f - friendliesAtHotZone * 0.55f,
            (fobBuilt ? 5f : 18f) + enemiesAtFob * 0.8f - friendliesAtFob * 0.45f,
            5f + enemiesAtRear * 0.9f - friendliesAtRear * 0.4f
        };
        TransportDestination[] destinations = TransportDestination.values();
        for (TransportWagon other : transportWagons) {
            if (other != wagon && other.faction == wagon.faction
                    && other.phase == TransportPhase.OUTBOUND) {
                scores[other.destinationMode.ordinal()] -= 12f;
            }
        }
        scores[wagon.routePreference] += 2.5f;

        int bestIndex = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[bestIndex]) {
                bestIndex = i;
            }
        }
        return destinations[bestIndex];
    }

    private int countFactionNear(Faction faction, Vector3f position, float radius) {
        int count = 0;
        for (BattleActor actor : allBattleActors()) {
            if (actor.alive && !actor.ridingWagon && actor.faction == faction
                    && distance2D(actor.model.getLocalTranslation(), position) <= radius) {
                count++;
            }
        }
        return count;
    }

    private int countEnemiesNear(Faction faction, Vector3f position, float radius) {
        int count = 0;
        for (BattleActor actor : allBattleActors()) {
            if (actor.alive && !actor.ridingWagon && actor.faction != faction
                    && distance2D(actor.model.getLocalTranslation(), position) <= radius) {
                count++;
            }
        }
        return count;
    }

    private void unloadWagon(TransportWagon wagon) {
        int index = 0;
        for (BattleActor passenger : wagon.passengers) {
            if (passenger == wagon.driver || !passenger.alive) {
                continue;
            }
            float angle = FastMath.TWO_PI * index / Math.max(1, wagon.passengers.size() - 1);
            Vector3f position = wagon.position.add(FastMath.cos(angle) * 3f, 0f,
                    FastMath.sin(angle) * 3f);
            passenger.model.setLocalTranslation(position);
            passenger.model.setCullHint(Spatial.CullHint.Inherit);
            passenger.ridingWagon = false;
            passenger.transportWagon = null;
            log(passenger.name + " 下車並投入戰鬥！");
            index++;
        }
        wagon.passengers.clear();
        wagon.passengers.add(wagon.driver);
        wagon.destination = wagon.pickup.clone();
        wagon.phase = TransportPhase.RETURNING;
        transportAnnouncement("[運輸兵喊話]：到了，全體下車作戰！");
    }

    private void transportAnnouncement(String message) {
        System.out.println(message);
        log(message);
    }

    private void createFactionBases() {
        for (Faction faction : Faction.values()) {
            Vector3f center = factionBases.get(faction);
            ColorRGBA color = factionColor(faction);
            Node base = new Node(faction.getDisplayName() + " safe spawn zone");

            Geometry floor = new Geometry("Safe zone floor",
                    new Cylinder(40, 40, FACTION_BASE_RADIUS, 0.07f, true));
            Material floorMaterial = unshadedMaterial(
                    new ColorRGBA(color.r, color.g, color.b, 0.24f));
            floorMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            floorMaterial.getAdditionalRenderState().setDepthWrite(false);
            floor.setMaterial(floorMaterial);
            floor.setLocalTranslation(0f, 0.08f, 0f);
            base.attachChild(floor);

            Material borderMaterial = unshadedMaterial(
                    new ColorRGBA(color.r, color.g, color.b, 0.95f));
            for (int i = 0; i < 32; i++) {
                float angle = FastMath.TWO_PI * i / 32f;
                Geometry marker = new Geometry("Safe zone border",
                        new Cylinder(8, 8, 0.2f, 0.13f));
                marker.setMaterial(borderMaterial);
                marker.setLocalTranslation(FastMath.cos(angle) * FACTION_BASE_RADIUS,
                        0.16f, FastMath.sin(angle) * FACTION_BASE_RADIUS);
                base.attachChild(marker);
            }

            Material beaconMaterial = unshadedMaterial(color);
            for (int i = 0; i < 4; i++) {
                float angle = FastMath.HALF_PI * i;
                Geometry beacon = new Geometry("Safe zone beacon",
                        new Box(0.16f, 1.1f, 0.16f));
                beacon.setMaterial(beaconMaterial);
                beacon.setLocalTranslation(FastMath.cos(angle) * 5.8f, 1.1f,
                        FastMath.sin(angle) * 5.8f);
                base.attachChild(beacon);
            }
            base.setLocalTranslation(center);
            rootNode.attachChild(base);
        }
    }

    private void createBattleSquad() {
        arrowShaftMaterial = unshadedMaterial(new ColorRGBA(0.52f, 0.32f, 0.14f, 1f));
        arrowTipMaterial = unshadedMaterial(new ColorRGBA(1f, 0.78f, 0.28f, 1f));
        Material redArmor = litMaterial(new ColorRGBA(0.82f, 0.16f, 0.13f, 1f));
        Material blueArmor = litMaterial(new ColorRGBA(0.12f, 0.48f, 0.92f, 1f));
        Material greenArmor = litMaterial(new ColorRGBA(0.15f, 0.62f, 0.28f, 1f));
        Material skin = litMaterial(new ColorRGBA(0.88f, 0.66f, 0.48f, 1f));
        Material redLegs = litMaterial(new ColorRGBA(0.3f, 0.08f, 0.08f, 1f));
        Material blueLegs = litMaterial(new ColorRGBA(0.1f, 0.2f, 0.36f, 1f));
        Material greenLegs = litMaterial(new ColorRGBA(0.08f, 0.28f, 0.12f, 1f));

        int redCount = TOTAL_BATTLE_AI / 3 + (TOTAL_BATTLE_AI % 3 > 0 ? 1 : 0);
        int greenCount = TOTAL_BATTLE_AI / 3 + (TOTAL_BATTLE_AI % 3 > 1 ? 1 : 0);
        int blueCount = TOTAL_BATTLE_AI / 3;
        createFactionSquad(Faction.RED, redCount, enemies,
                redArmor, skin, redLegs);
        createFactionSquad(Faction.GREEN, greenCount, greenForces,
                greenArmor, skin, greenLegs);
        createFactionSquad(Faction.BLUE, blueCount, allies,
                blueArmor, skin, blueLegs);
        log("山丘戰區啟動：100 名 AI（紅 " + redCount + "、綠 " + greenCount
                + "、藍 " + blueCount + "）配置多職業投入戰鬥。");
    }

    private void createFactionSquad(Faction faction, int count, List<BattleActor> roster,
                                    Material armor, Material skin, Material legs) {
        for (int i = 0; i < count; i++) {
            float angle = FastMath.TWO_PI * i / count;
            float radius = 3f + (i % 5) * 1.15f;
            JobType job = JobType.values()[i % JobType.values().length];
            Vector3f spawn = factionBases.get(faction).add(
                    FastMath.cos(angle) * radius, 0f, FastMath.sin(angle) * radius);
            BattleActor actor = createBattleActor(
                    faction.getDisplayName() + " " + job.getDisplayName() + "-" + (i + 1),
                    faction, job, spawn, armor, skin, legs);
            roster.add(actor);
        }
    }

    private BattleActor createBattleActor(String name, Faction faction, JobType job,
                                           Vector3f position,
                                           Material armor, Material skin, Material legs) {
        Node model = new Node(name);
        Node torso = new Node(name + " torso");
        torso.setLocalTranslation(0f, 1.25f, 0f);
        model.attachChild(torso);

        Geometry body = new Geometry("Body", new Box(0.34f, 0.48f, 0.24f));
        body.setMaterial(armor);
        body.setLocalTranslation(0f, -0.12f, 0f);
        torso.attachChild(body);

        Geometry head = new Geometry("Head", new Sphere(12, 12, 0.27f));
        head.setMaterial(skin);
        head.setLocalTranslation(0f, 0.55f, 0f);
        torso.attachChild(head);

        Node leftArm = createActorLimb("Left arm", -0.43f, 0.28f, armor,
                new Box(0.11f, 0.33f, 0.13f));
        Node rightArm = createActorLimb("Right arm", 0.43f, 0.28f, armor,
                new Box(0.11f, 0.33f, 0.13f));
        torso.attachChild(leftArm);
        torso.attachChild(rightArm);
        Node leftLeg = createActorLimb("Left leg", -0.17f, 0.72f, legs,
                new Box(0.13f, 0.34f, 0.15f));
        Node rightLeg = createActorLimb("Right leg", 0.17f, 0.72f, legs,
                new Box(0.13f, 0.34f, 0.15f));
        model.attachChild(leftLeg);
        model.attachChild(rightLeg);

        ColorRGBA teamColor = factionColor(faction);
        Geometry teamMark = new Geometry("Team marker", new Sphere(12, 12, 0.24f));
        teamMark.setMaterial(unshadedMaterial(teamColor));
        teamMark.setLocalTranslation(0f, 2.18f, 0f);
        model.attachChild(teamMark);
        Geometry jobMark = new Geometry("Job marker", new Sphere(8, 8, 0.13f));
        jobMark.setMaterial(unshadedMaterial(jobColor(job)));
        jobMark.setLocalTranslation(0f, 2.55f, 0f);
        model.attachChild(jobMark);
        Geometry shoulderStripe = new Geometry("Team shoulder stripe",
                new Box(0.37f, 0.12f, 0.27f));
        shoulderStripe.setMaterial(unshadedMaterial(teamColor));
        shoulderStripe.setLocalTranslation(0f, 0.06f, 0f);
        torso.attachChild(shoulderStripe);

        model.setLocalTranslation(position);
        rootNode.attachChild(model);
        return new BattleActor(name, faction, job, model, torso, leftArm, rightArm,
                leftLeg, rightLeg, position);
    }

    private Node createActorLimb(String name, float x, float y, Material material,
                                 com.jme3.scene.Mesh shape) {
        Node pivot = new Node(name + " joint");
        pivot.setLocalTranslation(x, y, 0f);
        Geometry limb = new Geometry(name, shape);
        limb.setMaterial(material);
        limb.setLocalTranslation(0f, -0.26f, 0f);
        pivot.attachChild(limb);
        return pivot;
    }

    private void updateBattleActors(float tpf) {
        List<BattleActor> actors = allBattleActors();
        for (BattleActor actor : actors) {
            if (!actor.alive) {
                actor.respawnTimer -= tpf;
                if (actor.respawnTimer <= 0f) {
                    respawnActor(actor);
                }
                continue;
            }
            if (actor.ridingWagon) {
                continue;
            }
            actor.attackTimer -= tpf;
            actor.stunTimer = Math.max(0f, actor.stunTimer - tpf);
            actor.supportTimer = Math.max(0f, actor.supportTimer - tpf);
            actor.blinkCooldown = Math.max(0f, actor.blinkCooldown - tpf);
            updateKnockback(actor.model, actor.knockbackVelocity, tpf, false, actor.faction);
            if (actor.stunTimer > 0f) {
                animateBattleActor(actor, 0f);
                continue;
            }

            TransportWagon boardingWagon = boardingWagonFor(actor);
            if (boardingWagon != null) {
                moveActorToward(actor, boardingWagon.position,
                        actor.job.getMovementSpeed(), tpf);
                continue;
            }

            ForwardBase forwardBase = forwardBases.get(actor.faction);
            if (!forwardBase.built && actor == forwardBase.builder) {
                if (forwardBase.building) {
                    animateBattleActor(actor, 0f);
                } else {
                    moveActorToward(actor, forwardBase.position,
                            actor.job.getMovementSpeed(), tpf);
                }
                continue;
            }
            if (actor.job == JobType.LOGISTICS) {
                supportNearbyAllies(actor);
            }
            if (actor.job == JobType.PRIEST) {
                supportWithPriest(actor);
            }
            if (actor.job == JobType.ENGINEER && forwardBase.built
                    && updateEngineer(actor, tpf)) {
                continue;
            }

            BattleActor target = nearestEnemy(actor, actors,
                    actor.job.getAttackRange() + 10f);
            Decoy decoy = nearestDecoy(actor);
            if (decoy != null && (target == null
                    || distance2D(actor.model.getLocalTranslation(), decoy.position)
                    < distance2D(actor.model.getLocalTranslation(),
                            target.model.getLocalTranslation()))) {
                if (distance2D(actor.model.getLocalTranslation(), decoy.position)
                        <= actor.job.getAttackRange()) {
                    decoy.life -= tpf;
                    if (decoy.life <= 0f) {
                        decoy.node.removeFromParent();
                        rogueDecoys.remove(decoy);
                    }
                } else {
                    moveActorToward(actor, decoy.position, actor.job.getMovementSpeed(), tpf);
                }
                continue;
            }

            Vector3f playerPosition = player.getLocalTranslation();
            float playerDistance = distance2D(actor.model.getLocalTranslation(), playerPosition);
            float threatRange = isPlayerNearHotZone()
                    ? HOT_ZONE_PLAYER_THREAT_RANGE : PLAYER_THREAT_RANGE;
            boolean targetPlayer = actor.faction != Faction.BLUE
                    && adventurer.isAlive()
                    && playerSpawnProtectionTimer <= 0f
                    && !isProtectedInBase(Faction.BLUE, playerPosition)
                    && playerDistance <= threatRange
                    && (target == null || isPlayerNearHotZone()
                    || playerDistance <= distance2D(actor.model.getLocalTranslation(),
                            target.model.getLocalTranslation()) + 2f);

            if (targetPlayer) {
                if (playerDistance <= actor.job.getAttackRange()) {
                    attackPlayer(actor);
                }
                if (shouldKite(actor, playerDistance)) {
                    moveActorToward(actor, positionAtKitingRange(playerPosition,
                            actor.model.getLocalTranslation(), kitingRange(actor)),
                            actor.job.getMovementSpeed(), tpf);
                } else if (playerDistance > actor.job.getAttackRange()) {
                    moveActorToward(actor, playerPosition, actor.job.getMovementSpeed(), tpf);
                }
            } else if (target != null) {
                float distance = distance2D(actor.model.getLocalTranslation(),
                        target.model.getLocalTranslation());
                if (actor.job == JobType.ROGUE && distance > actor.job.getAttackRange()
                        && tryRogueBlink(actor, target)) {
                    continue;
                }
                if (distance <= actor.job.getAttackRange()) {
                    attackActor(actor, target, actors);
                }
                if (shouldKite(actor, distance)) {
                    moveActorToward(actor, positionAtKitingRange(
                            target.model.getLocalTranslation(), actor.model.getLocalTranslation(),
                            kitingRange(actor)), actor.job.getMovementSpeed(), tpf);
                } else if (distance > actor.job.getAttackRange()) {
                    moveActorToward(actor, target.model.getLocalTranslation(),
                            actor.job.getMovementSpeed(), tpf);
                }
            } else {
                moveTowardJobObjective(actor, tpf, actors);
            }
        }
    }

    private TransportWagon boardingWagonFor(BattleActor actor) {
        for (TransportWagon wagon : transportWagons) {
            if (wagon.phase == TransportPhase.BOARDING
                    && wagon.boardingCandidates.contains(actor)) {
                return wagon;
            }
        }
        return null;
    }

    private void moveTowardJobObjective(BattleActor actor, float tpf, List<BattleActor> actors) {
        Vector3f objective = hotZoneNode.getLocalTranslation();
        ForwardBase forwardBase = forwardBases.get(actor.faction);
        if ((actor.job == JobType.ENGINEER || actor.job == JobType.LOGISTICS)
                && !forwardBase.built) {
            objective = forwardBase.position;
        } else if (actor.job == JobType.ARCHER || actor.job == JobType.MAGE
                || actor.job == JobType.PRIEST) {
            objective = selectHighGround(actor, actors);
        }
        if (actor.job == JobType.ENGINEER) {
            if (forwardBase.built) {
                Vector3f offset = actor.initialPosition.subtract(objective);
                if (offset.lengthSquared() < 0.01f) {
                    offset.set(1f, 0f, 0f);
                }
                offset.normalizeLocal().multLocal(5.5f);
                objective = objective.add(offset);
            }
        } else if (actor.job == JobType.LOGISTICS) {
            if (forwardBase.built) {
                BattleActor ally = nearestInjuredAlly(actor, actors);
                if (ally != null && distance2D(actor.model.getLocalTranslation(),
                        ally.model.getLocalTranslation()) > 3f) {
                    objective = ally.model.getLocalTranslation();
                } else {
                    objective = hotZoneNode.getLocalTranslation();
                }
            }
        }
        moveActorToward(actor, objective, actor.job.getMovementSpeed(), tpf);
    }

    private Vector3f selectHighGround(BattleActor actor, List<BattleActor> actors) {
        Vector3f current = actor.model.getLocalTranslation();
        Vector3f hotZone = hotZoneNode.getLocalTranslation();
        Vector3f best = highGroundPositions.get(0);
        float bestScore = Float.MAX_VALUE;
        for (Vector3f candidate : highGroundPositions) {
            float score = distance2D(current, candidate) * 0.35f
                    + distance2D(hotZone, candidate) * 0.65f;
            for (BattleActor enemy : actors) {
                if (enemy.alive && enemy.faction != actor.faction
                        && distance2D(enemy.model.getLocalTranslation(), candidate)
                                <= actor.job.getAttackRange() + 8f) {
                    score -= 2.5f;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private boolean isPlayerNearHotZone() {
        return distance2D(player.getLocalTranslation(), hotZoneNode.getLocalTranslation()) <= 12f;
    }

    private Vector3f positionAwayFrom(Vector3f threat, Vector3f current, float distance) {
        Vector3f direction = current.subtract(threat);
        if (direction.lengthSquared() < 0.01f) {
            direction.set(1f, 0f, 0f);
        }
        return current.add(direction.normalizeLocal().multLocal(distance));
    }

    private boolean shouldKite(BattleActor actor, float threatDistance) {
        return isRangedKiter(actor) && threatDistance < kitingRange(actor);
    }

    private boolean isRangedKiter(BattleActor actor) {
        return actor.job == JobType.MAGE || actor.job == JobType.ARCHER
                || actor.job == JobType.PRIEST;
    }

    private float kitingRange(BattleActor actor) {
        if (actor.job == JobType.ARCHER) {
            return 11f;
        }
        return actor.job == JobType.MAGE ? 8f : 7f;
    }

    private Vector3f positionAtKitingRange(Vector3f threat, Vector3f current, float range) {
        Vector3f direction = current.subtract(threat);
        if (direction.lengthSquared() < 0.01f) {
            direction.set(1f, 0f, 0f);
        }
        return threat.add(direction.normalizeLocal().multLocal(range));
    }

    private void attackActor(BattleActor attacker, BattleActor target,
                             List<BattleActor> actors) {
        faceActor(attacker.model, target.model.getLocalTranslation());
        animateBattleActor(attacker, 0f);
        if (attacker.attackTimer > 0f) {
            return;
        }
        attacker.attackTimer = attacker.job.getAttackCooldown();
        int damage = attacker.job.getDamage() + random.nextInt(5);
        if (attacker.job == JobType.ROGUE && isIsolated(target, actors)) {
            damage += 8;
        }
        if (attacker.job == JobType.ROGUE && tryRogueBlink(attacker, target)) {
            faceActor(attacker.model, target.model.getLocalTranslation());
        }
        if (attacker.job == JobType.ARCHER) {
            fireArrow(attacker, target, false);
            return;
        }
        if (attacker.job == JobType.MAGE) {
            createMagicBlast(target.model.getLocalTranslation(), 3.2f);
            for (BattleActor victim : actors) {
                if (victim.alive && !victim.ridingWagon
                        && victim.faction != attacker.faction
                        && !isProtectedInBase(victim.faction,
                                victim.model.getLocalTranslation())
                        && distance2D(target.model.getLocalTranslation(),
                                victim.model.getLocalTranslation()) <= 3.2f) {
                    damageActor(victim, damage, attacker.model.getLocalTranslation(), 0.8f);
                        createCombatText(victim.model.getLocalTranslation(), "-" + damage,
                                new ColorRGBA(1f, 0.55f, 0.22f, 1f));
                }
            }
            if (attacker.faction != Faction.BLUE
                    && target.faction != attacker.faction) {
                damagePlayerFromMageIfCaught(attacker, target, damage);
            }
            log(attacker.name + " 施放範圍法術！");
            return;
        }
        damageActor(target, damage, attacker.model.getLocalTranslation(),
                attacker.job.getKnockback());
    }

    private void damagePlayerFromMageIfCaught(BattleActor attacker,
                                               BattleActor target, int damage) {
        if (distance2D(player.getLocalTranslation(), target.model.getLocalTranslation()) <= 3.2f) {
            damagePlayer(attacker, damage, 0.8f);
        }
    }

    private void attackPlayer(BattleActor attacker) {
        if (attacker.faction == Faction.BLUE) {
            return;
        }
        faceActor(attacker.model, player.getLocalTranslation());
        animateBattleActor(attacker, 0f);
        if (attacker.attackTimer > 0f) {
            return;
        }
        attacker.attackTimer = attacker.job.getAttackCooldown();
        int damage = attacker.job.getDamage() + random.nextInt(5);
        if (attacker.job == JobType.ARCHER) {
            fireArrow(attacker, null, true);
            return;
        }
        if (attacker.job == JobType.MAGE) {
            damagePlayer(attacker, damage, 0.8f);
            log(attacker.name + " 對你施放法術！");
            return;
        }
        damagePlayer(attacker, damage, attacker.job.getKnockback());
    }

    private void fireArrow(BattleActor archer, BattleActor target, boolean targetPlayer) {
        if (targetPlayer && (archer.faction == Faction.BLUE
                || !adventurer.isAlive() || playerSpawnProtectionTimer > 0f
                || isProtectedInBase(Faction.BLUE, player.getLocalTranslation()))) {
            return;
        }
        if (target != null && (!target.alive
                || isProtectedInBase(target.faction, target.model.getLocalTranslation()))) {
            return;
        }

        Vector3f start = archer.model.getLocalTranslation().add(0f, 1.35f, 0f);
        Vector3f targetPosition = (targetPlayer ? player.getLocalTranslation()
                : target.model.getLocalTranslation()).add(0f, 1.25f, 0f);
        Vector3f direction = targetPosition.subtract(start);
        if (direction.lengthSquared() < 0.01f) {
            return;
        }
        direction.normalizeLocal();

        Node arrow = new Node("Archer arrow");
        Geometry shaft = new Geometry("Arrow shaft", new Box(0.045f, 0.045f, 0.48f));
        shaft.setMaterial(arrowShaftMaterial);
        arrow.attachChild(shaft);
        Geometry tip = new Geometry("Arrow tip", new Sphere(8, 8, 0.11f));
        tip.setMaterial(arrowTipMaterial);
        tip.setLocalTranslation(0f, 0f, 0.53f);
        arrow.attachChild(tip);
        arrow.setLocalTranslation(start);
        arrow.setLocalRotation(new Quaternion().fromAngleAxis(
                FastMath.atan2(direction.x, direction.z), Vector3f.UNIT_Y));
        rootNode.attachChild(arrow);

        projectiles.add(new Projectile(arrow, archer, target, targetPlayer,
                direction.mult(ARCHER_PROJECTILE_SPEED), start,
                archer.job.getDamage() + random.nextInt(3), 0f, archer.faction));
    }

    private void updateProjectiles(float tpf) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.life -= tpf;
            if (projectile.life <= 0f
                    || (!projectile.targetPlayer && !projectile.target.alive)
                    || (projectile.targetPlayer && !adventurer.isAlive())) {
                projectile.model.removeFromParent();
                projectiles.remove(i);
                continue;
            }

            Vector3f previous = projectile.model.getLocalTranslation();
            Vector3f next = previous.add(projectile.velocity.mult(tpf));
            if (isBlockedByWall(previous, next)) {
                projectile.model.removeFromParent();
                projectiles.remove(i);
                continue;
            }
            projectile.model.setLocalTranslation(next);
            faceActor(projectile.model, next.add(projectile.velocity));

            Vector3f targetPosition = (projectile.targetPlayer
                    ? player.getLocalTranslation()
                    : projectile.target.model.getLocalTranslation()).add(0f, 1.25f, 0f);
            if (distanceSquaredToSegment(targetPosition, previous, next) <= 0.8f * 0.8f) {
                if (projectile.blastRadius > 0f) {
                    applyStructureBlast(projectile, targetPosition);
                } else if (projectile.targetPlayer) {
                    if (playerSpawnProtectionTimer <= 0f
                            && !isProtectedInBase(Faction.BLUE, player.getLocalTranslation())) {
                        damagePlayer(projectile.owner, projectile.damage, 0.25f);
                    }
                } else if (!isProtectedInBase(projectile.target.faction,
                        projectile.target.model.getLocalTranslation())) {
                    damageActor(projectile.target, projectile.damage,
                            projectile.sourcePosition, 0.35f);
                }
                projectile.model.removeFromParent();
                projectiles.remove(i);
            }
        }
    }

    private boolean isBlockedByWall(Vector3f start, Vector3f end) {
        for (DefenseStructure structure : defenseStructures) {
            if (structure.type == DefenseType.WALL
                    && distanceSquaredToSegment(structure.position.add(0f, 1f, 0f),
                            start, end) < 1.1f * 1.1f) {
                return true;
            }
        }
        return false;
    }

    private void applyStructureBlast(Projectile projectile, Vector3f impact) {
        impact.setY(0f);
        createMagicBlast(impact, projectile.blastRadius);
        for (BattleActor victim : allBattleActors()) {
            if (victim.alive && !victim.ridingWagon
                    && victim.faction != projectile.faction
                    && !isProtectedInBase(victim.faction,
                            victim.model.getLocalTranslation())
                    && distance2D(impact, victim.model.getLocalTranslation())
                            <= projectile.blastRadius) {
                damageActor(victim, projectile.damage, projectile.sourcePosition, 0.5f);
            }
        }
        if (projectile.faction != Faction.BLUE && adventurer.isAlive()
                && distance2D(impact, player.getLocalTranslation()) <= projectile.blastRadius
                && !isProtectedInBase(Faction.BLUE, player.getLocalTranslation())) {
            damagePlayer(projectile.owner, projectile.damage, 0.5f);
        }
    }

    private float distanceSquaredToSegment(Vector3f point, Vector3f start, Vector3f end) {
        Vector3f segment = end.subtract(start);
        float lengthSquared = segment.lengthSquared();
        if (lengthSquared < 0.0001f) {
            return point.distanceSquared(start);
        }
        float fraction = FastMath.clamp(point.subtract(start).dot(segment) / lengthSquared,
                0f, 1f);
        Vector3f closest = start.add(segment.mult(fraction));
        return point.distanceSquared(closest);
    }

    private void damagePlayer(BattleActor attacker, int damage, float knockback) {
        if (attacker.faction == Faction.BLUE || !adventurer.isAlive()
                || playerSpawnProtectionTimer > 0f
                || isProtectedInBase(Faction.BLUE, player.getLocalTranslation())) {
            return;
        }
        adventurer.takeDamage(damage);
        Vector3f direction = player.getLocalTranslation()
                .subtract(attacker.model.getLocalTranslation());
        if (direction.lengthSquared() > 0.001f) {
            playerKnockback.addLocal(direction.normalizeLocal().multLocal(knockback * 2.2f));
        }
        playerStunTimer = Math.max(playerStunTimer, knockback > 0f ? 0.28f : 0.12f);
        if (playerHitFeedbackTimer <= 0f) {
            playerHitFeedbackTimer = 0.45f;
            playHitSound();
            log(attacker.name + " 命中你，造成 " + damage + " 點傷害！");
        }
        checkDefeat();
    }

    private void updateKnockback(Node target, Vector3f velocity, float tpf,
                                 boolean playerTarget, Faction targetFaction) {
        if (velocity.lengthSquared() < 0.01f) {
            velocity.set(0f, 0f, 0f);
            if (playerTarget) {
                playerStunTimer = Math.max(0f, playerStunTimer - tpf);
            }
            return;
        }
        Vector3f next = target.getLocalTranslation().add(velocity.mult(tpf));
        next.x = FastMath.clamp(next.x, -WORLD_LIMIT, WORLD_LIMIT);
        next.z = FastMath.clamp(next.z, -WORLD_LIMIT, WORLD_LIMIT);
        boolean allowed = playerTarget ? canMoveTo(next) : canMoveActorTo(next);
        if (allowed && isInsideEnemyBase(targetFaction, next)) {
            allowed = false;
        }
        if (allowed) {
            target.setLocalTranslation(next);
        }
        velocity.multLocal(Math.max(0f, 1f - 5f * tpf));
        if (playerTarget) {
            playerStunTimer = Math.max(0f, playerStunTimer - tpf);
        }
    }

    private BattleActor nearestInjuredAlly(BattleActor actor, List<BattleActor> actors) {
        BattleActor closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (BattleActor candidate : actors) {
            if (!candidate.alive || candidate.ridingWagon || candidate.faction != actor.faction
                    || candidate == actor || candidate.health >= candidate.job.getMaxHealth()) {
                continue;
            }
            float distance = distance2D(actor.model.getLocalTranslation(),
                    candidate.model.getLocalTranslation());
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean isBacklineJob(JobType job) {
        return job == JobType.MAGE || job == JobType.ARCHER || job == JobType.PRIEST;
    }

    private void supportNearbyAllies(BattleActor supporter) {
        if (supporter.attackTimer > 0f) {
            return;
        }
        supporter.attackTimer = 4f;
        for (BattleActor ally : allBattleActors()) {
            if (ally.faction != supporter.faction || ally == supporter) {
                continue;
            }
            if (!ally.alive && ally.respawnTimer > 1.5f
                    && !ally.ridingWagon
                    && distance2D(supporter.model.getLocalTranslation(),
                            ally.initialPosition) <= 12f) {
                ally.respawnTimer = Math.max(1.5f, ally.respawnTimer - 1f);
            } else if (ally.alive && !ally.ridingWagon
                    && ally.health < ally.job.getMaxHealth()
                    && distance2D(supporter.model.getLocalTranslation(),
                            ally.model.getLocalTranslation()) <= 5f) {
                ally.health = Math.min(ally.job.getMaxHealth(), ally.health + 12);
            }
        }
    }

    private void supportWithPriest(BattleActor priest) {
            if (priest.supportTimer > 0f) {
                return;
            }
            BattleActor patient = null;
            float lowestHealthRatio = 1f;
            for (BattleActor ally : allBattleActors()) {
                if (!ally.alive || ally.ridingWagon
                        || ally.faction != priest.faction || ally == priest
                        || distance2D(priest.model.getLocalTranslation(),
                                ally.model.getLocalTranslation()) > 8f) {
                    continue;
                }
                float healthRatio = (float) ally.health / ally.job.getMaxHealth();
                if (healthRatio < lowestHealthRatio) {
                    patient = ally;
                    lowestHealthRatio = healthRatio;
                }
            }
            boolean playerNeedsHealing = priest.faction == Faction.BLUE
                    && adventurer.isAlive()
                    && distance2D(priest.model.getLocalTranslation(),
                            player.getLocalTranslation()) <= 8f
                    && (float) adventurer.getHealth() / adventurer.getMaxHealth()
                            < lowestHealthRatio;
            if (playerNeedsHealing
                    && (float) adventurer.getHealth() / adventurer.getMaxHealth() < 0.88f) {
                int restored = Math.min(20,
                        adventurer.getMaxHealth() - adventurer.getHealth());
                if (restored > 0) {
                    adventurer.heal(restored);
                    priest.supportTimer = 3.5f;
                    createHealingEffect(player.getLocalTranslation(), restored);
                    playHealSound();
                    log(priest.name + " 治療你，恢復 " + restored + " 點生命！");
                }
                return;
            }
            if (patient == null || lowestHealthRatio >= 0.88f) {
                return;
            }
            priest.supportTimer = 3.5f;
            int restored = Math.min(24, patient.job.getMaxHealth() - patient.health);
            patient.health += restored;
            createHealingEffect(patient, restored);
            playHealSound();
            log(priest.name + " 施放治療術，" + patient.name + " 恢復 " + restored + " 點生命！");
        }

        private boolean updateEngineer(BattleActor engineer, float tpf) {
            engineer.buildCooldown = Math.max(0f, engineer.buildCooldown - tpf);
            if (engineer.buildCooldown > 0f) {
                return false;
            }
            if (engineer.building) {
                engineer.buildTimer -= tpf;
                if (engineer.buildMarker != null) {
                    float progress = 1f - Math.max(0f, engineer.buildTimer)
                            / engineer.buildDuration;
                    engineer.buildMarker.setLocalScale(0.7f + progress * 0.55f,
                            0.7f + progress * 0.55f, 0.7f + progress * 0.55f);
                }
                if (engineer.buildTimer <= 0f) {
                    completeEngineerBuild(engineer);
                }
                return true;
            }

            if (engineer.buildSite == null) {
                engineer.buildSite = findEngineerBuildSite(engineer);
            }
            if (distance2D(engineer.model.getLocalTranslation(), engineer.buildSite) > 1.7f) {
                moveActorToward(engineer, engineer.buildSite, engineer.job.getMovementSpeed(), tpf);
                return true;
            }
            engineer.building = true;
            engineer.buildDuration = 3f + random.nextFloat() * 2f;
            engineer.buildTimer = engineer.buildDuration;
            engineer.buildType = DefenseType.values()[engineer.buildIndex % DefenseType.values().length];
            engineer.buildMarker = createConstructionMarker(engineer);
            log(engineer.name + " 開始建造" + engineer.buildType.label
                    + "（" + (int) Math.ceil(engineer.buildDuration) + " 秒）。");
            return true;
        }

        private Vector3f findEngineerBuildSite(BattleActor engineer) {
            Vector3f center = hotZoneNode.getLocalTranslation();
            float factionAngle = engineer.faction == Faction.RED ? FastMath.PI
                    : engineer.faction == Faction.GREEN ? FastMath.HALF_PI : 0f;
            int ordinal = engineer.buildIndex + countFactionEngineersBefore(engineer);
            for (int attempt = 0; attempt < 12; attempt++) {
                float angle = factionAngle + ordinal * 1.35f + attempt * FastMath.HALF_PI;
                Vector3f candidate = center.add(FastMath.cos(angle) * 8.5f, 0f,
                        FastMath.sin(angle) * 8.5f);
                if (canMoveActorTo(candidate) && !isInsideEnemyBase(engineer.faction, candidate)
                        && !isNearStructure(candidate, 2.5f)) {
                    return candidate;
                }
            }
            return engineer.model.getLocalTranslation().clone();
        }

        private int countFactionEngineersBefore(BattleActor engineer) {
            int count = 0;
            for (BattleActor candidate : allBattleActors()) {
                if (candidate == engineer) {
                    return count;
                }
                if (candidate.faction == engineer.faction
                        && candidate.job == JobType.ENGINEER) {
                    count++;
                }
            }
            return count;
        }

        private boolean isNearStructure(Vector3f position, float distance) {
            for (DefenseStructure structure : defenseStructures) {
                if (distance2D(position, structure.position) < distance) {
                    return true;
                }
            }
            return false;
        }

        private Node createConstructionMarker(BattleActor engineer) {
            Node marker = new Node("Construction in progress");
            Geometry frame = new Geometry("Construction frame", new Box(0.8f, 0.7f, 0.8f));
            Material material = unshadedMaterial(new ColorRGBA(1f, 0.82f, 0.22f, 0.55f));
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            material.getAdditionalRenderState().setWireframe(true);
            frame.setMaterial(material);
            frame.setLocalTranslation(0f, 0.8f, 0f);
            marker.attachChild(frame);
            marker.setLocalTranslation(engineer.buildSite);
            rootNode.attachChild(marker);
            return marker;
        }

        private void completeEngineerBuild(BattleActor engineer) {
            if (engineer.buildMarker != null) {
                engineer.buildMarker.removeFromParent();
                engineer.buildMarker = null;
            }
            createDefenseStructure(engineer, engineer.buildType, engineer.buildSite);
            engineer.building = false;
            engineer.buildSite = null;
            engineer.buildCooldown = 14f;
            engineer.buildIndex++;
            engineer.buildTimer = 0f;
            log(engineer.name + " 完成建造：" + engineer.buildType.label + "！");
        }

        private void createDefenseStructure(BattleActor builder, DefenseType type,
                                            Vector3f position) {
            Node model = new Node(type.label);
            Geometry body;
            if (type == DefenseType.WALL) {
                body = new Geometry("Defense wall", new Box(1.8f, 1.25f, 0.38f));
                body.setMaterial(litMaterial(factionColor(builder.faction)));
                body.setLocalTranslation(0f, 1.25f, 0f);
            } else if (type == DefenseType.ARCHER_TOWER) {
                body = new Geometry("Archer tower", new Cylinder(12, 12, 0.42f, 3.6f));
                body.setMaterial(litMaterial(new ColorRGBA(0.54f, 0.36f, 0.2f, 1f)));
                body.setLocalTranslation(0f, 1.8f, 0f);
            } else {
                body = new Geometry("Artillery turret", new Cylinder(16, 16, 0.72f, 1.25f));
                body.setMaterial(litMaterial(new ColorRGBA(0.3f, 0.34f, 0.37f, 1f)));
                body.setLocalTranslation(0f, 0.65f, 0f);
            }
            model.attachChild(body);
            Geometry teamLight = new Geometry("Structure team light", new Sphere(8, 8, 0.24f));
            teamLight.setMaterial(unshadedMaterial(factionColor(builder.faction)));
            teamLight.setLocalTranslation(0f, type == DefenseType.ARCHER_TOWER ? 3.8f : 1.8f, 0f);
            model.attachChild(teamLight);
            model.setLocalTranslation(position);
            rootNode.attachChild(model);

            DefenseStructure structure = new DefenseStructure(builder.faction, type, model,
                    position, builder);
            defenseStructures.add(structure);
            if (type == DefenseType.WALL) {
                worldObstacles.add(new WorldObstacle(position.x, position.z + 0.2f, 1.25f,
                        1.8f, 2.5f));
                structure.wallObstacle = worldObstacles.get(worldObstacles.size() - 1);
            }
        }

        private void updateDefenseStructures(float tpf) {
            List<BattleActor> actors = allBattleActors();
            for (DefenseStructure structure : defenseStructures) {
                if (structure.type == DefenseType.WALL) {
                    continue;
                }
                structure.attackTimer -= tpf;
                if (structure.attackTimer > 0f) {
                    continue;
                }
                BattleActor target = nearestStructureTarget(structure, actors,
                        structure.type == DefenseType.ARCHER_TOWER ? 18f : 11f);
                boolean targetPlayer = structure.faction != Faction.BLUE
                        && adventurer.isAlive() && playerSpawnProtectionTimer <= 0f
                        && !isProtectedInBase(Faction.BLUE, player.getLocalTranslation())
                        && distance2D(structure.position, player.getLocalTranslation()) <= 14f
                        && (target == null || distance2D(structure.position,
                                player.getLocalTranslation()) <= distance2D(structure.position,
                                target.model.getLocalTranslation()));
                if (target == null && !targetPlayer) {
                    continue;
                }
                structure.attackTimer = structure.type == DefenseType.ARCHER_TOWER ? 1.8f : 4.5f;
                fireStructureProjectile(structure, target, targetPlayer);
            }
        }

        private BattleActor nearestStructureTarget(DefenseStructure structure,
                                                    List<BattleActor> actors, float range) {
            BattleActor nearest = null;
            float closestDistance = range;
            for (BattleActor candidate : actors) {
                if (!candidate.alive || candidate.ridingWagon
                        || candidate.faction == structure.faction
                        || isProtectedInBase(candidate.faction,
                                candidate.model.getLocalTranslation())) {
                    continue;
                }
                float distance = distance2D(structure.position, candidate.model.getLocalTranslation());
                if (distance < closestDistance) {
                    nearest = candidate;
                    closestDistance = distance;
                }
            }
            return nearest;
        }

        private void fireStructureProjectile(DefenseStructure structure, BattleActor target,
                                             boolean targetPlayer) {
            Vector3f targetPosition = targetPlayer ? player.getLocalTranslation()
                    : target.model.getLocalTranslation();
            Vector3f start = structure.position.add(0f,
                    structure.type == DefenseType.ARCHER_TOWER ? 3f : 1.4f, 0f);
            Vector3f direction = targetPosition.add(0f, 1.1f, 0f).subtract(start);
            if (direction.lengthSquared() < 0.01f) {
                return;
            }
            direction.normalizeLocal();
            Node projectileNode = new Node(structure.type == DefenseType.TURRET
                    ? "Turret shell" : "Tower arrow");
            Geometry projectileGeometry = new Geometry("Defense projectile",
                    structure.type == DefenseType.TURRET
                            ? new Sphere(8, 8, 0.24f) : new Box(0.06f, 0.06f, 0.42f));
            projectileGeometry.setMaterial(structure.type == DefenseType.TURRET
                    ? unshadedMaterial(new ColorRGBA(1f, 0.42f, 0.12f, 1f))
                    : arrowTipMaterial);
            projectileNode.attachChild(projectileGeometry);
            projectileNode.setLocalTranslation(start);
            rootNode.attachChild(projectileNode);
            int damage = structure.type == DefenseType.TURRET ? 30 : 14;
            projectiles.add(new Projectile(projectileNode, structure.builder, target,
                    targetPlayer, direction.mult(structure.type == DefenseType.TURRET ? 13f : 20f),
                    start, damage, structure.type == DefenseType.TURRET ? 3f : 0f,
                    structure.faction));
        }

        private void createMagicBlast(Vector3f position, float radius) {
            Node effect = new Node("Flame storm blast");
            Geometry groundMark = new Geometry("Blast area",
                    new Cylinder(32, 32, radius, 0.08f, true));
            Material groundMaterial = unshadedMaterial(new ColorRGBA(1f, 0.3f, 0.04f, 0.48f));
            groundMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            groundMaterial.getAdditionalRenderState().setDepthWrite(false);
            groundMark.setMaterial(groundMaterial);
            groundMark.setLocalTranslation(0f, 0.12f, 0f);
            effect.attachChild(groundMark);

            Geometry flare = new Geometry("Blast flare", new Sphere(12, 12, 1f));
            Material flareMaterial = unshadedMaterial(new ColorRGBA(1f, 0.72f, 0.12f, 0.75f));
            flareMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            flareMaterial.getAdditionalRenderState().setDepthWrite(false);
            flare.setMaterial(flareMaterial);
            flare.setLocalTranslation(0f, 1f, 0f);
            flare.setLocalScale(0.35f);
            effect.attachChild(flare);
            effect.setLocalTranslation(position);
            rootNode.attachChild(effect);
            battleEffects.add(new BattleEffect(effect, flare, null, position,
                    new ColorRGBA(1f, 0.75f, 0.22f, 1f), 1.1f, 0.85f));
            createCombatText(position.add(0f, 3.2f, 0f), "✦ 烈焰風暴 ✦",
                    new ColorRGBA(1f, 0.58f, 0.12f, 1f));
        }

        private void createHealingEffect(BattleActor patient, int restored) {
            createHealingEffect(patient.model.getLocalTranslation(), restored);
        }

        private void createHealingEffect(Vector3f position, int restored) {
            Node effect = new Node("Healing aura");
            Geometry aura = new Geometry("Healing glow", new Sphere(12, 12, 1f));
            Material glowMaterial = unshadedMaterial(new ColorRGBA(0.12f, 1f, 0.42f, 0.5f));
            glowMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            glowMaterial.getAdditionalRenderState().setDepthWrite(false);
            aura.setMaterial(glowMaterial);
            aura.setLocalTranslation(0f, 1.1f, 0f);
            aura.setLocalScale(0.65f, 1.2f, 0.65f);
            effect.attachChild(aura);
            effect.setLocalTranslation(position);
            rootNode.attachChild(effect);
            battleEffects.add(new BattleEffect(effect, aura, null,
                    position, new ColorRGBA(0.2f, 1f, 0.45f, 1f),
                    1f, 1.1f));
            createCombatText(position.add(0f, 3f, 0f),
                    "+" + restored + " HP", new ColorRGBA(0.2f, 1f, 0.45f, 1f));
        }

        private void createShadowTrail(Vector3f origin, Vector3f destination, Faction faction) {
            for (int i = 0; i < 4; i++) {
                float fraction = (i + 1f) / 5f;
                Vector3f position = origin.clone().interpolateLocal(destination, fraction);
                Node shadow = new Node("Rogue shadow trail");
                Geometry silhouette = new Geometry("Shadow afterimage", new Sphere(10, 10, 1f));
                Material material = unshadedMaterial(new ColorRGBA(0.35f, 0.14f, 0.72f, 0.42f));
                material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                material.getAdditionalRenderState().setDepthWrite(false);
                silhouette.setMaterial(material);
                silhouette.setLocalScale(0.42f, 1.15f, 0.38f);
                silhouette.setLocalTranslation(0f, 1.05f, 0f);
                shadow.attachChild(silhouette);
                shadow.setLocalTranslation(position);
                rootNode.attachChild(shadow);
                battleEffects.add(new BattleEffect(shadow, silhouette, null, position,
                        new ColorRGBA(0.55f, 0.3f, 1f, 1f), 0.5f, 1.2f));
                if (i == 0) {
                    rogueDecoys.add(new Decoy(shadow, position, faction, 1.2f));
                }
            }
        }

        private Decoy nearestDecoy(BattleActor actor) {
            Decoy closest = null;
            float closestDistance = 14f;
            for (Decoy decoy : rogueDecoys) {
                if (decoy.faction == actor.faction || decoy.life <= 0f) {
                    continue;
                }
                float distance = distance2D(actor.model.getLocalTranslation(), decoy.position);
                if (distance < closestDistance) {
                    closest = decoy;
                    closestDistance = distance;
                }
            }
            return closest;
        }

        private void updateRogueDecoys(float tpf) {
            for (int i = rogueDecoys.size() - 1; i >= 0; i--) {
                Decoy decoy = rogueDecoys.get(i);
                decoy.life -= tpf;
                if (decoy.life <= 0f) {
                    decoy.node.removeFromParent();
                    rogueDecoys.remove(i);
                }
            }
        }

        private void createCombatText(Vector3f position, String message, ColorRGBA color) {
            BitmapText text = new BitmapText(guiFont);
            text.setSize(18f);
            text.setText(message);
            text.setColor(color);
            guiNode.attachChild(text);
            battleEffects.add(new BattleEffect(null, null, text, position, color, 0f, 1.1f));
        }

        private void updateBattleEffects(float tpf) {
            for (int i = battleEffects.size() - 1; i >= 0; i--) {
                BattleEffect effect = battleEffects.get(i);
                effect.life -= tpf;
                float progress = 1f - Math.max(0f, effect.life) / effect.duration;
                if (effect.label != null) {
                    Vector3f world = effect.position.add(0f, 3f + progress * 1.4f, 0f);
                    Vector3f screen = cam.getScreenCoordinates(world);
                    effect.label.setLocalTranslation(screen.x, screen.y, 0f);
                    effect.label.setColor(new ColorRGBA(effect.color.r, effect.color.g,
                            effect.color.b, Math.max(0f, 1f - progress)));
                }
                if (effect.geometry != null) {
                    if (effect.radius > 1f) {
                        float pulse = effect.radius * (0.35f + progress * 0.9f);
                        effect.geometry.setLocalScale(pulse);
                    }
                }
                if (effect.life <= 0f) {
                    if (effect.node != null) {
                        effect.node.removeFromParent();
                    }
                    if (effect.label != null) {
                        effect.label.removeFromParent();
                    }
                    battleEffects.remove(i);
                }
            }
        }

        private boolean tryRogueBlink(BattleActor rogue, BattleActor target) {
            if (rogue.blinkCooldown > 0f) {
                return false;
            }
            float healthRatio = (float) rogue.health / rogue.job.getMaxHealth();
            Vector3f roguePosition = rogue.model.getLocalTranslation();
            Vector3f targetPosition = target.model.getLocalTranslation();
            float distance = distance2D(roguePosition, targetPosition);
            boolean lowHealthEscape = healthRatio <= 0.35f;
            boolean flank = distance >= 3.5f && distance <= 9f;
            if (!lowHealthEscape && !flank) {
                return false;
            }

            Vector3f destination;
            if (lowHealthEscape) {
                Vector3f escapeDirection = roguePosition.subtract(targetPosition);
                if (escapeDirection.lengthSquared() < 0.01f) {
                    escapeDirection.set(1f, 0f, 0f);
                }
                destination = targetPosition.add(
                        escapeDirection.normalizeLocal().multLocal(4f));
            } else {
                Vector3f targetForward = target.model.getWorldRotation().mult(Vector3f.UNIT_Z);
                destination = targetPosition.subtract(targetForward.mult(1.7f));
            }
            destination.y = 0f;
            if (isInsideEnemyBase(rogue.faction, destination)
                    || !canMoveActorTo(destination)) {
                return false;
            }
            Vector3f origin = roguePosition.clone();
            createShadowTrail(origin, destination, rogue.faction);
            rogue.model.setLocalTranslation(destination);
            rogue.knockbackVelocity.set(0f, 0f, 0f);
            rogue.stunTimer = 0f;
            rogue.blinkCooldown = 8f;
            if (lowHealthEscape) {
                log(rogue.name + " 殘血閃現脫離戰線！");
            } else {
                log(rogue.name + " 閃現繞至 " + target.name + " 身後！");
            }
            return true;
        }
    private boolean isIsolated(BattleActor target, List<BattleActor> actors) {
        for (BattleActor nearby : actors) {
            if (nearby.alive && nearby.faction == target.faction && nearby != target
                    && distance2D(nearby.model.getLocalTranslation(),
                            target.model.getLocalTranslation()) <= 5f) {
                return false;
            }
        }
        return true;
    }

    private void updateHotZone(float tpf) {
        if (hotZoneRetargetTimer >= HOT_ZONE_TARGET_INTERVAL_SECONDS) {
            hotZoneRetargetTimer = 0f;
            chooseNextHotZoneTarget();
        }
        Vector3f position = hotZoneNode.getLocalTranslation();
        Vector3f delta = new Vector3f(hotZoneTarget.x - position.x, 0f,
                hotZoneTarget.z - position.z);
        float distance = delta.length();
        if (distance > 0.01f) {
            float step = Math.min(distance, HOT_ZONE_MOVE_SPEED * tpf);
            position.addLocal(delta.normalizeLocal().multLocal(step));
            hotZoneNode.setLocalTranslation(position);
        }
    }

    private void chooseNextHotZoneTarget() {
        hillZoneIndex = (hillZoneIndex + 1) % HILL_ZONE_NAMES.length;
        currentHotZoneLabel = HILL_ZONE_NAMES[hillZoneIndex];
        float angle = random.nextFloat() * FastMath.TWO_PI;
        float radius = 5f + random.nextFloat() * (CONTROL_AREA_RADIUS - 12f);
        hotZoneTarget.set(FastMath.cos(angle) * radius, 0f, FastMath.sin(angle) * radius);
        log("熱區正移往「" + currentHotZoneLabel + "」！");
    }

    private void scoreHillControl() {
        EnumMap<Faction, Integer> votes = new EnumMap<>(Faction.class);
        for (Faction faction : Faction.values()) {
            votes.put(faction, 0);
        }
        for (BattleActor actor : allBattleActors()) {
            if (!actor.alive || actor.ridingWagon) {
                continue;
            }
            Vector3f position = actor.model.getLocalTranslation();
            if (position.x * position.x + position.z * position.z
                    > CONTROL_AREA_RADIUS * CONTROL_AREA_RADIUS) {
                continue;
            }
            boolean insideHotZone = distance2D(position, hotZoneNode.getLocalTranslation()) <= 5.2f;
            votes.put(actor.faction, votes.get(actor.faction) + (insideHotZone ? 2 : 1));
        }

        Faction winner = null;
        int highest = 0;
        boolean tied = false;
        for (Faction faction : Faction.values()) {
            int factionVotes = votes.get(faction);
            if (factionVotes > highest) {
                winner = faction;
                highest = factionVotes;
                tied = false;
            } else if (factionVotes == highest && factionVotes > 0) {
                tied = true;
            }
        }

        hillLeader = highest == 0 || tied ? null : winner;
        if (hillLeader == null) {
            log("山丘之王結算：票數平手，紅 " + votes.get(Faction.RED)
                    + "／綠 " + votes.get(Faction.GREEN)
                    + "／藍 " + votes.get(Faction.BLUE) + "，本輪不計分。");
            updateHud();
            return;
        }

        int newScore = factionScores.get(hillLeader) + 1;
        factionScores.put(hillLeader, newScore);
        log(hillLeader.getDisplayName() + " 控制山丘！加權票數 "
                + votes.get(Faction.RED) + "/" + votes.get(Faction.GREEN)
                + "/" + votes.get(Faction.BLUE) + "，目前 " + newScore
                + "/" + HILL_TARGET_SCORE + " 分。");
        playRewardSound();
        if (newScore >= HILL_TARGET_SCORE) {
            gameStates.transitionTo(GameStateManager.State.VICTORY);
            log(hillLeader.getDisplayName() + " 陣營率先達到 "
                    + HILL_TARGET_SCORE + " 分，贏得山丘之王！");
        }
        updateHud();
    }

    private void respawnActor(BattleActor actor) {
        actor.health = actor.job.getMaxHealth();
        actor.alive = true;
        actor.attackTimer = 1f;
        actor.stunTimer = 0f;
        actor.knockbackVelocity.set(0f, 0f, 0f);
        ForwardBase forwardBase = forwardBases.get(actor.faction);
        Vector3f spawn = forwardBase != null && forwardBase.built
                ? forwardBase.position : actor.initialPosition;
        if (isInsideEnemyBase(actor.faction, spawn)) {
            spawn = actor.initialPosition;
        }
        actor.model.setLocalTranslation(spawn);
        actor.model.setCullHint(Spatial.CullHint.Inherit);
        log(actor.name + (forwardBase != null && forwardBase.built
                ? " 已在前線 FOB 重生！" : " 已從後方重生！"));
        updateHud();
    }

    private List<BattleActor> allBattleActors() {
        List<BattleActor> actors = new ArrayList<>(allies.size() + enemies.size() + greenForces.size());
        actors.addAll(allies);
        actors.addAll(enemies);
        actors.addAll(greenForces);
        return actors;
    }

    private BattleActor nearestEnemy(BattleActor attacker, List<BattleActor> actors, float range) {
        BattleActor closest = null;
        BattleActor preferredBackline = null;
        float closestDistance = range;
        float closestBacklineDistance = range;
        Vector3f position = attacker.model.getLocalTranslation();
        for (BattleActor candidate : actors) {
            if (!candidate.alive || candidate.ridingWagon
                    || candidate.faction == attacker.faction
                    || isProtectedInBase(candidate.faction,
                            candidate.model.getLocalTranslation())) {
                continue;
            }
            float distance = distance2D(position, candidate.model.getLocalTranslation());
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
            if (attacker.job == JobType.ROGUE && isBacklineJob(candidate.job)
                    && distance < closestBacklineDistance) {
                preferredBackline = candidate;
                closestBacklineDistance = distance;
            }
        }
        return preferredBackline != null ? preferredBackline : closest;
    }

    private BattleActor nearestEnemy(Vector3f position, float range) {
        BattleActor closest = null;
        float closestDistance = range;
        for (BattleActor candidate : allBattleActors()) {
            if (!candidate.alive || candidate.ridingWagon || candidate.faction == Faction.BLUE
                    || isProtectedInBase(candidate.faction,
                            candidate.model.getLocalTranslation())) {
                continue;
            }
            float distance = distance2D(position, candidate.model.getLocalTranslation());
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private void moveActorToward(BattleActor actor, Vector3f target, float speed, float tpf) {
        Vector3f current = actor.model.getLocalTranslation();
        Vector3f direction = new Vector3f(target.x - current.x, 0f, target.z - current.z);
        if (direction.lengthSquared() < 0.25f) {
            actor.stuckTimer = 0f;
            actor.recoveryTimer = 0f;
            actor.hasNavigationWaypoint = false;
            animateBattleActor(actor, 0f);
            return;
        }
        direction.normalizeLocal();
        Vector3f targetDelta = target.subtract(actor.lastMoveTarget);
        if (targetDelta.lengthSquared() > 4f) {
            actor.stuckTimer = 0f;
            actor.recoveryTimer = 0f;
            actor.hasNavigationWaypoint = false;
            actor.lastMoveTarget.set(target);
            actor.lastMovePosition.set(current);
            actor.moveCheckTimer = 0f;
        }

        actor.recoveryTimer = Math.max(0f, actor.recoveryTimer - tpf);
        actor.moveCheckTimer += tpf;
        if (actor.moveCheckTimer >= 0.45f) {
            Vector3f progressTarget = actor.hasNavigationWaypoint
                    ? actor.navigationWaypoint : target;
            float progress = distance2D(actor.lastMovePosition, progressTarget)
                    - distance2D(current, progressTarget);
            if (progress < 0.035f && actor.recoveryTimer <= 0f) {
                actor.stuckTimer += actor.moveCheckTimer;
            } else {
                actor.stuckTimer = 0f;
            }
            actor.lastMovePosition.set(current);
            actor.moveCheckTimer = 0f;
        }

        if (actor.stuckTimer >= 0.85f && !actor.hasNavigationWaypoint) {
            actor.navigationWaypoint.set(findDetourWaypoint(actor, target, direction));
            actor.hasNavigationWaypoint = actor.navigationWaypoint.distanceSquared(current) > 0.1f;
            actor.stuckTimer = 0f;
            actor.recoveryTimer = 0.8f;
            actor.lastMovePosition.set(current);
        }

        if (actor.hasNavigationWaypoint) {
            Vector3f waypointDelta = actor.navigationWaypoint.subtract(current);
            if (waypointDelta.lengthSquared() < 0.8f
                    || isPathClear(current, target, actor.faction)) {
                actor.hasNavigationWaypoint = false;
            } else {
                direction.set(waypointDelta.normalizeLocal());
            }
        }

        if (actor.stuckTimer >= 2.4f) {
            Vector3f escapePosition = findEmergencyEscapePosition(actor, target);
            if (escapePosition != null) {
                actor.model.setLocalTranslation(escapePosition);
                current = actor.model.getLocalTranslation();
                actor.lastMovePosition.set(current);
                actor.stuckTimer = 0f;
                actor.moveCheckTimer = 0f;
                actor.hasNavigationWaypoint = false;
                actor.recoveryTimer = 0f;
            }
        }

        if (actor.recoveryTimer > 0f && !actor.hasNavigationWaypoint) {
            if (actor.stuckTimer >= 0.9f) {
                actor.recoveryDirection.set(chooseRecoveryDirection(actor, direction, target));
                actor.recoveryTimer = 1.1f;
                actor.stuckTimer = 0f;
                actor.recoverySide *= -1f;
            }
            if (actor.recoveryDirection.lengthSquared() > 0.01f) {
                direction.set(actor.recoveryDirection);
            }
        }

        Vector3f next = current.add(direction.mult(speed * tpf));
        next.x = FastMath.clamp(next.x, -WORLD_LIMIT, WORLD_LIMIT);
        next.z = FastMath.clamp(next.z, -WORLD_LIMIT, WORLD_LIMIT);
        if (canMoveActorTo(next) && !isInsideEnemyBase(actor.faction, next)) {
            actor.model.setLocalTranslation(next);
            actor.blockedMoveTimer = 0f;
        } else {
            actor.stuckTimer += tpf;
            actor.blockedMoveTimer += tpf;
            actor.recoveryTimer = 0f;
            actor.hasNavigationWaypoint = false;
        }
        faceActor(actor.model, target);
        animateBattleActor(actor, animationTime * 9f + actor.animationOffset);
    }

    private Vector3f findDetourWaypoint(BattleActor actor, Vector3f target,
                                        Vector3f desired) {
        Vector3f current = actor.model.getLocalTranslation();
        Vector3f best = new Vector3f(current);
        float bestScore = Float.MAX_VALUE;
        float[] radii = {2f, 3.5f, 5f, 7f, 9f};
        for (float radius : radii) {
            for (int i = 0; i < 24; i++) {
                float angle = FastMath.TWO_PI * i / 24f
                        + (actor.recoverySide < 0f ? FastMath.PI / 24f : 0f);
                Vector3f candidate = current.add(FastMath.cos(angle) * radius, 0f,
                        FastMath.sin(angle) * radius);
                if (!canMoveActorTo(candidate) || isInsideEnemyBase(actor.faction, candidate)
                        || !isPathClear(current, candidate, actor.faction)) {
                    continue;
                }
                float remaining = distance2D(candidate, target);
                float progress = distance2D(current, target) - remaining;
                boolean clearToTarget = isPathClear(candidate, target, actor.faction);
                float score = remaining - progress * 0.35f + (clearToTarget ? 0f : 2.5f)
                        - desired.dot(candidate.subtract(current).normalizeLocal()) * 0.6f;
                if (score < bestScore) {
                    bestScore = score;
                    best.set(candidate);
                }
            }
            if (bestScore < Float.MAX_VALUE && distance2D(best, target) < distance2D(current, target)) {
                break;
            }
        }
        return best;
    }

    private boolean isPathClear(Vector3f start, Vector3f end, Faction faction) {
        Vector3f delta = end.subtract(start);
        float distance = delta.length();
        if (distance < 0.1f) {
            return true;
        }
        delta.normalizeLocal();
        int checks = Math.max(1, (int) Math.ceil(distance / 0.35f));
        for (int i = 1; i <= checks; i++) {
            Vector3f point = start.add(delta.mult(distance * i / checks));
            if (!canMoveActorTo(point) || isInsideEnemyBase(faction, point)) {
                return false;
            }
        }
        return true;
    }

    private Vector3f findEmergencyEscapePosition(BattleActor actor, Vector3f target) {
        Vector3f current = actor.model.getLocalTranslation();
        Vector3f best = null;
        float bestDistance = distance2D(current, target);
        float[] radii = {1.25f, 1.75f, 2.5f, 3.5f, 4.5f};
        for (float radius : radii) {
            for (int i = 0; i < 32; i++) {
                float angle = FastMath.TWO_PI * i / 32f;
                Vector3f candidate = current.add(FastMath.cos(angle) * radius, 0f,
                        FastMath.sin(angle) * radius);
                if (!canMoveActorTo(candidate) || isInsideEnemyBase(actor.faction, candidate)) {
                    continue;
                }
                float distance = distance2D(candidate, target);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
            if (best != null) {
                break;
            }
        }
        if (best != null) {
            log(actor.name + " 遭遇障礙，執行緊急脫困移動。");
        }
        return best;
    }

    private Vector3f chooseRecoveryDirection(BattleActor actor, Vector3f desired,
                                             Vector3f target) {
        float[] angles = {45f, -45f, 90f, -90f, 135f, -135f, 180f};
        Vector3f current = actor.model.getLocalTranslation();
        Vector3f bestDirection = new Vector3f();
        float bestScore = -Float.MAX_VALUE;
        float probeDistance = 1.2f;
        for (float angle : angles) {
            Quaternion turn = new Quaternion().fromAngleAxis(
                    FastMath.DEG_TO_RAD * angle * actor.recoverySide, Vector3f.UNIT_Y);
            Vector3f candidateDirection = turn.mult(desired).normalizeLocal();
            Vector3f probe = current.add(candidateDirection.mult(probeDistance));
            probe.x = FastMath.clamp(probe.x, -WORLD_LIMIT, WORLD_LIMIT);
            probe.z = FastMath.clamp(probe.z, -WORLD_LIMIT, WORLD_LIMIT);
            if (!canMoveActorTo(probe) || isInsideEnemyBase(actor.faction, probe)) {
                continue;
            }
            float progress = distance2D(current, target) - distance2D(probe, target);
            float score = progress + candidateDirection.dot(desired) * 0.35f
                    + random.nextFloat() * 0.08f;
            if (score > bestScore) {
                bestScore = score;
                bestDirection.set(candidateDirection);
            }
        }
        if (bestScore == -Float.MAX_VALUE) {
            Vector3f fallback = new Vector3f(-desired.z, 0f, desired.x)
                    .multLocal(actor.recoverySide);
            return fallback;
        }
        return bestDirection;
    }

    private boolean canMoveActorTo(Vector3f candidate) {
        if (FastMath.abs(candidate.x) > WORLD_LIMIT || FastMath.abs(candidate.z) > WORLD_LIMIT) {
            return false;
        }
        float actorBottom = 0f;
        float actorTop = 2.2f;
        for (WorldObstacle obstacle : worldObstacles) {
            float obstacleBottom = obstacle.centerY - obstacle.height * 0.5f;
            float obstacleTop = obstacle.centerY + obstacle.height * 0.5f;
            if (actorTop < obstacleBottom || actorBottom > obstacleTop) {
                continue;
            }
            float dx = candidate.x - obstacle.x;
            float dz = candidate.z - obstacle.z;
            float minimumDistance = 0.42f + obstacle.radius;
            if (dx * dx + dz * dz < minimumDistance * minimumDistance) {
                return false;
            }
        }
        return true;
    }

    private boolean isOccupiedByActor(Vector3f position, BattleActor movingActor) {
        for (BattleActor actor : allies) {
            if (actor != movingActor && actor.alive && !actor.ridingWagon
                    && distance2D(position, actor.model.getLocalTranslation()) < 0.85f) {
                return true;
            }
        }
        for (BattleActor actor : enemies) {
            if (actor != movingActor && actor.alive && !actor.ridingWagon
                    && distance2D(position, actor.model.getLocalTranslation()) < 0.85f) {
                return true;
            }
        }
        for (BattleActor actor : greenForces) {
            if (actor != movingActor && actor.alive && !actor.ridingWagon
                    && distance2D(position, actor.model.getLocalTranslation()) < 0.85f) {
                return true;
            }
        }
        return false;
    }

    private Faction baseOwnerAt(Vector3f position) {
        for (Faction faction : Faction.values()) {
            Vector3f base = factionBases.get(faction);
            if (base != null && distance2D(position, base) <= FACTION_BASE_RADIUS) {
                return faction;
            }
        }
        return null;
    }

    private boolean isProtectedInBase(Faction faction, Vector3f position) {
        return baseOwnerAt(position) == faction;
    }

    private boolean isInsideEnemyBase(Faction faction, Vector3f position) {
        Faction owner = baseOwnerAt(position);
        return owner != null && owner != faction;
    }

    private void faceActor(Node model, Vector3f target) {
        Vector3f direction = new Vector3f(target.x - model.getLocalTranslation().x, 0f,
                target.z - model.getLocalTranslation().z);
        if (direction.lengthSquared() > 0.001f) {
            float heading = FastMath.atan2(direction.x, direction.z);
            model.setLocalRotation(new Quaternion().fromAngleAxis(heading, Vector3f.UNIT_Y));
        }
    }

    private float distance2D(Vector3f first, Vector3f second) {
        float dx = first.x - second.x;
        float dz = first.z - second.z;
        return FastMath.sqrt(dx * dx + dz * dz);
    }

    private void animateBattleActor(BattleActor actor, float phase) {
        float swing = phase == 0f ? 0f : FastMath.sin(phase) * 0.48f;
        actor.leftLeg.setLocalRotation(new Quaternion().fromAngles(swing, 0f, 0f));
        actor.rightLeg.setLocalRotation(new Quaternion().fromAngles(-swing, 0f, 0f));
        actor.leftArm.setLocalRotation(new Quaternion().fromAngles(-swing * 0.75f, 0f, 0f));
        actor.rightArm.setLocalRotation(new Quaternion().fromAngles(swing * 0.75f, 0f, 0f));
    }

    private void damageActor(BattleActor actor, int damage) {
        damageActor(actor, damage, player.getLocalTranslation(), 2.6f);
    }

    private void damageActor(BattleActor actor, int damage, Vector3f sourcePosition,
                             float knockback) {
        if (!actor.alive || actor.ridingWagon || isProtectedInBase(actor.faction,
                actor.model.getLocalTranslation())) {
            return;
        }
        actor.health = Math.max(0, actor.health - damage);
        Vector3f direction = actor.model.getLocalTranslation().subtract(sourcePosition);
        if (knockback > 0f && direction.lengthSquared() > 0.001f) {
            actor.knockbackVelocity.addLocal(direction.normalizeLocal().multLocal(knockback));
            actor.stunTimer = Math.max(actor.stunTimer, 0.2f);
        }
        playHitSound();
        if (actor.health == 0) {
            actor.alive = false;
            ForwardBase forwardBase = forwardBases.get(actor.faction);
            actor.respawnTimer = forwardBase != null && forwardBase.built
                    ? FOB_RESPAWN_SECONDS : AI_RESPAWN_SECONDS;
            actor.model.setCullHint(Spatial.CullHint.Always);
            log(actor.name + " 陣亡，"
                    + (forwardBase != null && forwardBase.built
                            ? "2 秒後將從前線 FOB 重生！" : "5 秒後將從後方重生！"));
        }
    }

    private int countAlive(List<BattleActor> actors) {
        int count = 0;
        for (BattleActor actor : actors) {
            if (actor.alive) {
                count++;
            }
        }
        return count;
    }

    private int countRespawning(List<BattleActor> actors) {
        int count = 0;
        for (BattleActor actor : actors) {
            if (!actor.alive) {
                count++;
            }
        }
        return count;
    }

    private ColorRGBA factionColor(Faction faction) {
        if (faction == Faction.RED) {
            return new ColorRGBA(1f, 0.22f, 0.16f, 1f);
        }
        if (faction == Faction.GREEN) {
            return new ColorRGBA(0.18f, 0.95f, 0.32f, 1f);
        }
        return new ColorRGBA(0.2f, 0.65f, 1f, 1f);
    }

    private ColorRGBA jobColor(JobType job) {
        if (job == JobType.WARRIOR) {
            return new ColorRGBA(0.95f, 0.22f, 0.16f, 1f);
        }
        if (job == JobType.MAGE) {
            return new ColorRGBA(0.7f, 0.38f, 1f, 1f);
        }
        if (job == JobType.ARCHER) {
            return new ColorRGBA(0.25f, 0.9f, 0.45f, 1f);
        }
        if (job == JobType.ENGINEER) {
            return new ColorRGBA(1f, 0.75f, 0.18f, 1f);
        }
        if (job == JobType.LOGISTICS) {
            return new ColorRGBA(0.25f, 0.85f, 0.92f, 1f);
        }
        if (job == JobType.PRIEST) {
            return new ColorRGBA(0.48f, 1f, 0.58f, 1f);
        }
        return new ColorRGBA(1f, 0.4f, 0.72f, 1f);
    }

    private java.awt.Color factionAwtColor(Faction faction) {
        if (faction == Faction.RED) {
            return new java.awt.Color(255, 94, 82);
        }
        if (faction == Faction.GREEN) {
            return new java.awt.Color(89, 231, 122);
        }
        return new java.awt.Color(83, 174, 255);
    }

    private AnimComposer findAnimator(Spatial spatial) {
        AnimComposer composer = spatial.getControl(AnimComposer.class);
        if (composer != null) {
            return composer;
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                composer = findAnimator(child);
                if (composer != null) {
                    return composer;
                }
            }
        }
        return null;
    }

    private void configureCharacterAnimations() {
        List<String> clipNames = new ArrayList<>(playerAnimator.getAnimClipsNames());
        System.out.println("Character animation clips: " + clipNames);
        idleAnimation = findAnimation(clipNames, "idle", "stand", "breathing");
        runAnimation = findAnimation(clipNames, "run", "running", "walk", "walking");

        if (idleAnimation == null) {
            log("找不到 Idle 動畫。請將待機動畫命名為 Idle，或確認 GLB 內含該動畫。");
        }
        if (runAnimation == null) {
            log("找不到 Run 動畫。請將跑步動畫命名為 Running，或確認 GLB 內含該動畫。");
        }
        setLocomotionState(LocomotionState.IDLE);
    }

    private String findAnimation(List<String> clipNames, String... aliases) {
        for (String clipName : clipNames) {
            String normalizedName = clipName.toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
            for (String alias : aliases) {
                String normalizedAlias = alias.toLowerCase(java.util.Locale.ROOT);
                if (normalizedName.equals(normalizedAlias)
                        || normalizedName.contains(normalizedAlias)) {
                    return clipName;
                }
            }
        }
        return null;
    }

    private void updateLocomotionAnimation() {
        LocomotionState nextState = movement.lengthSquared() > 0f
                ? LocomotionState.RUNNING : LocomotionState.IDLE;
        setLocomotionState(nextState);
    }

    private void setLocomotionState(LocomotionState nextState) {
        if (locomotionState == nextState) {
            return;
        }
        locomotionState = nextState;
        if (playerAnimator == null) {
            return;
        }

        String animation = nextState == LocomotionState.RUNNING ? runAnimation : idleAnimation;
        if (animation != null) {
            playerAnimator.setCurrentAction(animation);
        }
    }

    private void createFallbackCharacter() {
        proceduralCharacter = true;
        Material armor = litMaterial(new ColorRGBA(0.2f, 0.42f, 0.72f, 1f));
        Material skin = litMaterial(new ColorRGBA(0.88f, 0.66f, 0.48f, 1f));
        Material dark = litMaterial(new ColorRGBA(0.15f, 0.18f, 0.22f, 1f));

        torsoPivot = new Node("Animated torso joint");
        torsoPivot.setLocalTranslation(0f, 1.35f, 0f);
        player.attachChild(torsoPivot);

        Geometry body = new Geometry("Body", new Box(0.42f, 0.62f, 0.26f));
        body.setMaterial(armor);
        body.setLocalTranslation(0f, -0.18f, 0f);
        torsoPivot.attachChild(body);

        Geometry head = new Geometry("Head", new Sphere(16, 16, 1f));
        head.setMaterial(skin);
        head.setLocalScale(0.32f);
        head.setLocalTranslation(0f, 0.68f, 0f);
        torsoPivot.attachChild(head);

        leftArmPivot = createLimbPivot("Left arm joint", -0.55f, 0.36f, armor,
                new Box(0.14f, 0.43f, 0.16f));
        rightArmPivot = createLimbPivot("Right arm joint", 0.55f, 0.36f, armor,
                new Box(0.14f, 0.43f, 0.16f));
        torsoPivot.attachChild(leftArmPivot);
        torsoPivot.attachChild(rightArmPivot);
        Geometry swordBlade = new Geometry("Sword blade", new Box(0.09f, 0.52f, 0.07f));
        swordBlade.setMaterial(litMaterial(new ColorRGBA(0.78f, 0.86f, 0.94f, 1f)));
        swordBlade.setLocalTranslation(0.02f, -0.72f, 0.12f);
        rightArmPivot.attachChild(swordBlade);
        Geometry swordGrip = new Geometry("Sword grip", new Box(0.13f, 0.08f, 0.09f));
        swordGrip.setMaterial(litMaterial(new ColorRGBA(0.83f, 0.61f, 0.2f, 1f)));
        swordGrip.setLocalTranslation(0.02f, -0.25f, 0.12f);
        rightArmPivot.attachChild(swordGrip);

        leftLegPivot = createLimbPivot("Left leg joint", -0.23f, 0.78f, dark,
                new Box(0.16f, 0.42f, 0.18f));
        rightLegPivot = createLimbPivot("Right leg joint", 0.23f, 0.78f, dark,
                new Box(0.16f, 0.42f, 0.18f));
        player.attachChild(leftLegPivot);
        player.attachChild(rightLegPivot);

        Geometry belt = new Geometry("Belt", new Box(0.44f, 0.1f, 0.28f));
        belt.setMaterial(dark);
        belt.setLocalTranslation(0f, -0.62f, 0f);
        torsoPivot.attachChild(belt);
        locomotionState = null;
        updateLocomotionAnimation();
    }

    private Node createLimbPivot(String name, float x, float y, Material material,
                                 com.jme3.scene.Mesh shape) {
        Node pivot = new Node(name);
        pivot.setLocalTranslation(x, y, 0f);
        Geometry limb = new Geometry(name + " mesh", shape);
        limb.setMaterial(material);
        limb.setLocalTranslation(0f, -0.34f, 0f);
        pivot.attachChild(limb);
        return pivot;
    }

    private boolean canMoveTo(Vector3f candidate) {
        if (FastMath.abs(candidate.x) > WORLD_LIMIT || FastMath.abs(candidate.z) > WORLD_LIMIT
                || isInsideEnemyBase(Faction.BLUE, candidate)) {
            return false;
        }
        float playerBottom = 0f;
        float playerTop = 2.2f;
        for (WorldObstacle obstacle : worldObstacles) {
            float obstacleBottom = obstacle.centerY - obstacle.height * 0.5f;
            float obstacleTop = obstacle.centerY + obstacle.height * 0.5f;
            if (playerTop < obstacleBottom || playerBottom > obstacleTop) {
                continue;
            }
            float dx = candidate.x - obstacle.x;
            float dz = candidate.z - obstacle.z;
            float minimumDistance = PLAYER_COLLIDER_RADIUS + obstacle.radius;
            if (dx * dx + dz * dz < minimumDistance * minimumDistance) {
                return false;
            }
        }
        if (monsterModel != null) {
            Vector3f monsterPosition = monsterModel.getLocalTranslation();
            float dx = candidate.x - monsterPosition.x;
            float dz = candidate.z - monsterPosition.z;
            if (dx * dx + dz * dz < MONSTER_CONTACT_DISTANCE * MONSTER_CONTACT_DISTANCE) {
                return false;
            }
        }
        if (isOccupiedByActor(candidate, null)) {
            return false;
        }
        return true;
    }

    private void updateMonsterContact(float tpf) {
        if (currentMonster == null || !adventurer.isAlive() || monsterModel == null) {
            monsterContactTimer = 0f;
            return;
        }
        monsterContactTimer -= tpf;
        Vector3f playerPosition = player.getLocalTranslation();
        Vector3f monsterPosition = monsterModel.getLocalTranslation();
        float dx = playerPosition.x - monsterPosition.x;
        float dz = playerPosition.z - monsterPosition.z;
        if (dx * dx + dz * dz <= MONSTER_CONTACT_DISTANCE * MONSTER_CONTACT_DISTANCE
                && monsterContactTimer <= 0f) {
            monsterContactTimer = 1.25f;
            int damage = currentMonster.attack();
            adventurer.takeDamage(damage);
            playHitSound();
            log(currentMonster.getName() + " 撞到你，造成 " + damage + " 點傷害！");
            checkDefeat();
            updateHud();
        }
    }

    private BoundingBox createWeaponHitbox() {
        Vector3f center = player.getLocalTranslation()
                .add(facingDirection.mult(0.95f))
                .add(0f, 1.05f, 0f);
        float halfForward = 1.2f;
        float halfSide = 0.72f;
        float halfX = FastMath.abs(facingDirection.x) * halfForward
                + FastMath.abs(facingDirection.z) * halfSide;
        float halfZ = FastMath.abs(facingDirection.z) * halfForward
                + FastMath.abs(facingDirection.x) * halfSide;
        return new BoundingBox(center, halfX, 0.7f, halfZ);
    }

    private BoundingBox createMonsterHitbox() {
        Vector3f center = monsterModel.getLocalTranslation().add(0f, 1f, 0f);
        return new BoundingBox(center, 0.9f, 1.1f, 0.85f);
    }

    private BoundingBox createActorHitbox(BattleActor actor) {
        Vector3f center = actor.model.getLocalTranslation().add(0f, 1f, 0f);
        return new BoundingBox(center, 0.55f, 1.1f, 0.5f);
    }

    private void updateProceduralAnimation() {
        if (!proceduralCharacter) {
            return;
        }

        boolean moving = locomotionState == LocomotionState.RUNNING;
        float runPhase = animationTime * (sprinting ? 14f : 10f);
        float legSwing = moving ? FastMath.sin(runPhase) * 0.72f : 0f;
        float armSwing = moving ? FastMath.sin(runPhase + FastMath.PI) * 0.62f : 0f;
        float idleBreath = moving ? 0f : FastMath.sin(animationTime * 2.2f) * 0.035f;
        float attackAmount = attackAnimationTime <= 0f ? 0f
                : FastMath.sin((attackAnimationTime / 0.42f) * FastMath.PI) * 1.25f;

        leftLegPivot.setLocalRotation(new Quaternion().fromAngles(legSwing, 0f, 0f));
        rightLegPivot.setLocalRotation(new Quaternion().fromAngles(-legSwing, 0f, 0f));
        leftArmPivot.setLocalRotation(new Quaternion().fromAngles(
                armSwing + attackAmount * 0.15f, 0f, 0f));
        rightArmPivot.setLocalRotation(new Quaternion().fromAngles(
                armSwing - attackAmount, 0f, 0f));
        torsoPivot.setLocalTranslation(0f, 1.35f + idleBreath
                + (moving ? FastMath.abs(FastMath.sin(runPhase * 2f)) * 0.07f : 0f), 0f);
        torsoPivot.setLocalRotation(new Quaternion().fromAngles(
                moving ? FastMath.sin(runPhase) * 0.04f : 0f,
                0f,
                moving ? FastMath.sin(runPhase) * 0.035f : 0f));
    }

    private void configureInput() {
        inputManager.addMapping("MoveLeft",
                new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("MoveRight",
                new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("MoveForward",
                new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("MoveBackward",
                new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT),
                new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("Explore", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Attack", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Flee", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Potion", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("BuyPotion", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("MenuClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener,
                "MoveLeft", "MoveRight", "MoveForward", "MoveBackward", "Sprint",
                "Explore", "Attack", "Flee", "Potion", "BuyPotion", "MenuClick");
    }

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if ("MenuClick".equals(name) && isPressed) {
                handleMenuClick();
                return;
            }
            if (!gameStates.isPlaying()) {
                return;
            }
            if ("MoveLeft".equals(name)) {
                keysDown[0] = isPressed;
            } else if ("MoveRight".equals(name)) {
                keysDown[1] = isPressed;
            } else if ("MoveForward".equals(name)) {
                keysDown[2] = isPressed;
            } else if ("MoveBackward".equals(name)) {
                keysDown[3] = isPressed;
            } else if ("Sprint".equals(name)) {
                sprinting = isPressed;
            } else if (isPressed && "Explore".equals(name)) {
                explore();
            } else if (isPressed && "Attack".equals(name)) {
                attack();
            } else if (isPressed && "Flee".equals(name)) {
                flee();
            } else if (isPressed && "Potion".equals(name)) {
                usePotion();
            } else if (isPressed && "BuyPotion".equals(name)) {
                buyPotion();
            }
        }
    };

    private void updateCamera() {
        Vector3f playerPosition = player.getLocalTranslation();
        Vector3f target = playerPosition.add(0f, 1f, -3.5f);
        cam.setLocation(playerPosition.add(0f, 11f, 17f));
        cam.lookAt(target, Vector3f.UNIT_Y);
    }

    private void createHud() {
        BufferedImage image = new BufferedImage(cam.getWidth(), cam.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        hudTexture = new Texture2D(new AWTLoader().load(image, true));
        Material hudMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        hudMaterial.setTexture("ColorMap", hudTexture);
        hudMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        hudMaterial.getAdditionalRenderState().setDepthTest(false);
        hudMaterial.getAdditionalRenderState().setDepthWrite(false);

        hudGeometry = new Geometry("Adventure HUD", new Quad(cam.getWidth(), cam.getHeight()));
        hudGeometry.setMaterial(hudMaterial);
        hudGeometry.setLocalTranslation(0f, 0f, 1f);
        guiNode.attachChild(hudGeometry);
        renderHud();
    }

    private void handleMenuClick() {
        if (gameStates.isPlaying()) {
            return;
        }
        com.jme3.math.Vector2f cursor = inputManager.getCursorPosition();
        float x = cursor.x;
        float y = cam.getHeight() - cursor.y;
        for (MenuButton button : menuButtons) {
            if (button.contains(x, y)) {
                activateMenuButton(button.action);
                return;
            }
        }
    }

    private void activateMenuButton(MenuAction action) {
        if (action == MenuAction.START || action == MenuAction.RESTART) {
            startNewGame();
        } else if (action == MenuAction.MAIN_MENU) {
            endBattle();
            merchantAvailable = false;
            gameStates.transitionTo(GameStateManager.State.MAIN_MENU);
            log("回到主選單。");
            updateHud();
        } else if (action == MenuAction.EXIT) {
            stop();
        }
    }

    private void startNewGame() {
        endBattle();
        for (MapEvent event : mapEvents) {
            event.node.removeFromParent();
        }
        mapEvents.clear();
        merchantAvailable = false;
        adventurer.reset();
        score = 0;
        defeatedMonsters = 0;
        for (Faction faction : Faction.values()) {
            factionScores.put(faction, 0);
        }
        playerKnockback.set(0f, 0f, 0f);
        playerStunTimer = 0f;
        playerHitFeedbackTimer = 0f;
        playerRespawnTimer = 0f;
        playerSpawnProtectionTimer = PLAYER_RESPAWN_PROTECTION_SECONDS;
        clearBattlefieldStructures();
        hillLeader = null;
        hillScoreTimer = 0f;
        hotZoneRetargetTimer = 0f;
        hotZoneNode.setLocalTranslation(0f, 0f, 0f);
        hotZoneTarget.set(0f, 0f, 0f);
        currentHotZoneLabel = "中央山丘";
        hillZoneIndex = 1;
        for (int i = 0; i < keysDown.length; i++) {
            keysDown[i] = false;
        }
        sprinting = false;
        player.setLocalTranslation(factionBases.get(Faction.BLUE));
        player.setCullHint(Spatial.CullHint.Inherit);
        facingDirection.set(0f, 0f, -1f);
        resetBattleActors();
        gameStates.transitionTo(GameStateManager.State.PLAYING);
        messages.clear();
        log("冒險開始！靠近彩色探索標記，按 E 進行探索。");
        populateMapEvents();
        updateHud();
        updateNearbyEvent();
    }

    private void resetBattleActors() {
        for (BattleActor actor : allBattleActors()) {
            actor.reset();
            actor.model.setLocalTranslation(actor.initialPosition);
        }
        createTransportWagons();
    }

    private void clearBattlefieldStructures() {
        for (TransportWagon wagon : transportWagons) {
            wagon.model.removeFromParent();
            for (BattleActor passenger : wagon.passengers) {
                passenger.ridingWagon = false;
            }
            wagon.driver.transportWagon = null;
        }
        transportWagons.clear();
        for (ForwardBase forwardBase : forwardBases.values()) {
            if (forwardBase.model != null) {
                forwardBase.model.removeFromParent();
            }
            if (forwardBase.marker != null) {
                forwardBase.marker.removeFromParent();
            }
            forwardBase.builder = null;
            forwardBase.model = null;
            forwardBase.marker = null;
            forwardBase.built = false;
            forwardBase.building = false;
            forwardBase.buildRemaining = FOB_BUILD_SECONDS;
        }
        for (DefenseStructure structure : defenseStructures) {
            structure.model.removeFromParent();
            if (structure.wallObstacle != null) {
                worldObstacles.remove(structure.wallObstacle);
            }
        }
        defenseStructures.clear();
        for (Projectile projectile : projectiles) {
            projectile.model.removeFromParent();
        }
        projectiles.clear();
        for (BattleEffect effect : battleEffects) {
            if (effect.node != null) {
                effect.node.removeFromParent();
            }
            if (effect.label != null) {
                effect.label.removeFromParent();
            }
        }
        battleEffects.clear();
        for (Decoy decoy : rogueDecoys) {
            decoy.node.removeFromParent();
        }
        rogueDecoys.clear();
    }

    private void renderHud() {
        if (hudTexture == null) {
            return;
        }

        int width = cam.getWidth();
        int height = cam.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        menuButtons.clear();
        if (gameStates.getCurrentState() != GameStateManager.State.PLAYING) {
            drawGameStateScreen(graphics, width, height);
            graphics.dispose();
            hudTexture.setImage(new AWTLoader().load(image, true));
            return;
        }

        drawPanel(graphics, 24, 22, 510, 380, new java.awt.Color(15, 24, 36, 222));
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 25));
        graphics.setColor(new java.awt.Color(255, 221, 145));
        graphics.drawString("冒險者之旅", 46, 57);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 15));
        graphics.setColor(new java.awt.Color(221, 230, 239));
        graphics.drawString("探索森林，尋找寶箱與未知事件", 48, 82);

        graphics.setColor(new java.awt.Color(53, 63, 75));
        graphics.fillRoundRect(46, 98, 300, 17, 9, 9);
        float healthRatio = (float) adventurer.getHealth() / adventurer.getMaxHealth();
        graphics.setColor(healthRatio > 0.35f
                ? new java.awt.Color(73, 204, 132) : new java.awt.Color(235, 86, 79));
        graphics.fillRoundRect(46, 98, Math.round(300 * healthRatio), 17, 9, 9);
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 14));
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString("生命 " + adventurer.getHealth() + " / "
                + adventurer.getMaxHealth(), 360, 112);

        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 15));
        graphics.setColor(new java.awt.Color(224, 232, 241));
        graphics.drawString("攻擊 " + adventurer.getAttackPower()
                + "     藥水 " + adventurer.getPotionCount()
                + "     金幣 " + adventurer.getCoins(), 48, 143);
        String monsterStatus = currentMonster == null
                ? "附近沒有怪物"
                : currentMonster.getName() + " 生命 "
                        + currentMonster.getHealth() + "/" + currentMonster.getMaxHealth();
        graphics.setColor(currentMonster == null
                ? new java.awt.Color(171, 187, 201) : new java.awt.Color(255, 133, 120));
        graphics.drawString(monsterStatus, 48, 173);
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 15));
        graphics.setColor(new java.awt.Color(255, 221, 145));
        graphics.drawString("分數 " + score + "     擊敗敵人 " + defeatedMonsters, 48, 198);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        graphics.setColor(gameAudio == null
                ? new java.awt.Color(255, 160, 135) : new java.awt.Color(151, 210, 174));
        graphics.drawString(audioStatus, 48, 226);
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 14));
        graphics.setColor(factionAwtColor(Faction.RED));
        graphics.drawString("紅 " + factionScores.get(Faction.RED), 48, 254);
        graphics.setColor(factionAwtColor(Faction.GREEN));
        graphics.drawString("綠 " + factionScores.get(Faction.GREEN), 132, 254);
        graphics.setColor(factionAwtColor(Faction.BLUE));
        graphics.drawString("藍 " + factionScores.get(Faction.BLUE), 216, 254);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        graphics.setColor(new java.awt.Color(220, 230, 239));
        graphics.drawString("熱區：" + currentHotZoneLabel
                + "　30 秒結算　勝利目標 " + HILL_TARGET_SCORE, 48, 278);
        graphics.drawString("高地火力點 " + highGroundPositions.size() + "　紅FOB "
                + forwardBaseStatus(Faction.RED) + "　綠FOB "
                + forwardBaseStatus(Faction.GREEN) + "　藍FOB "
                + forwardBaseStatus(Faction.BLUE), 48, 388);
        graphics.drawString("馬車運輸　紅 " + transportStatus(Faction.RED) + "　綠 "
                + transportStatus(Faction.GREEN) + "　藍 " + transportStatus(Faction.BLUE),
                48, 410);
        graphics.setColor(new java.awt.Color(147, 222, 255));
        graphics.drawString("各運輸兵依熱區戰況、FOB與敵情自行選擇馬車路線", 48, 432);
        graphics.drawString("紅 " + countAlive(enemies) + "存活/" + countRespawning(enemies)
                + "重生　綠 " + countAlive(greenForces) + "存活/"
                + countRespawning(greenForces) + "重生　藍 "
                + countAlive(allies) + "存活/" + countRespawning(allies)
                + "重生　總AI " + (enemies.size() + greenForces.size() + allies.size()), 48, 300);
        graphics.setColor(new java.awt.Color(220, 230, 239));
        graphics.drawString(!adventurer.isAlive()
                        ? "玩家重生倒數 " + Math.max(0, (int) Math.ceil(playerRespawnTimer)) + " 秒"
                        : playerSpawnProtectionTimer > 0f
                        ? "藍隊安全區／出生保護 " + (int) Math.ceil(playerSpawnProtectionTimer) + " 秒"
                        : "安全區：各隊邊緣基地有保護，敵隊不能進入",
                48, 322);
        graphics.drawString("職業 " + jobCompositionText(), 48, 344);
        graphics.setColor(new java.awt.Color(255, 222, 135));
        graphics.drawString("工事 " + constructionSummary(), 48, 366);

        int logHeight = Math.min(174, 48 + messages.size() * 23);
        int logY = height - logHeight - 22;
        drawPanel(graphics, 24, logY, Math.min(670, width - 48), logHeight,
                new java.awt.Color(15, 24, 36, 222));
        graphics.setFont(new Font("Dialog", Font.BOLD, 16));
        graphics.setColor(new java.awt.Color(255, 221, 145));
        graphics.drawString("冒險紀錄", 44, logY + 27);
        graphics.setFont(new Font("Dialog", Font.PLAIN, 15));
        graphics.setColor(new java.awt.Color(238, 242, 247));
        FontMetrics metrics = graphics.getFontMetrics();
        int lineY = logY + 52;
        int maxTextWidth = Math.min(625, width - 100);
        for (String message : messages) {
            for (String line : wrapText(message, metrics, maxTextWidth)) {
                if (lineY > height - 34) {
                    break;
                }
                graphics.drawString(line, 44, lineY);
                lineY += 21;
            }
        }

        String prompt = currentMonster != null
                ? "SPACE 攻擊    H 使用藥水    Q 逃跑"
                : merchantAvailable ? "M 購買藥水    E 離開商人" : promptMessage;
        java.awt.Color promptAwtColor = new java.awt.Color(
                Math.round(promptColor.r * 255f),
                Math.round(promptColor.g * 255f),
                Math.round(promptColor.b * 255f));
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 17));
        FontMetrics promptMetrics = graphics.getFontMetrics();
        int promptWidth = Math.min(width - 48, Math.max(330, promptMetrics.stringWidth(prompt) + 52));
        int promptX = (width - promptWidth) / 2;
        drawPanel(graphics, promptX, height - 72, promptWidth, 48,
                new java.awt.Color(15, 24, 36, 230));
        graphics.setColor(promptAwtColor);
        graphics.drawString(prompt, promptX + 24, height - 42);

        drawControls(graphics, width);
        drawMiniMap(graphics, width);
        graphics.dispose();
        hudTexture.setImage(new AWTLoader().load(image, true));
    }

    private void drawGameStateScreen(Graphics2D graphics, int width, int height) {
        graphics.setColor(new java.awt.Color(7, 13, 23, 185));
        graphics.fillRect(0, 0, width, height);

        int panelWidth = Math.min(560, width - 48);
        int panelHeight = 430;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight,
                new java.awt.Color(17, 27, 42, 244));

        GameStateManager.State state = gameStates.getCurrentState();
        String title;
        String subtitle;
        ColorRGBA accent;
        if (state == GameStateManager.State.MAIN_MENU) {
            title = "冒險者之旅";
            subtitle = "探索森林，參與紅、綠、藍三方山丘爭奪！";
            accent = new ColorRGBA(1f, 0.84f, 0.46f, 1f);
        } else if (state == GameStateManager.State.VICTORY) {
            title = "山丘之王：勝利";
            subtitle = hillLeader == null ? "戰區結算完成。"
                    : hillLeader.getDisplayName() + " 陣營率先取得 " + HILL_TARGET_SCORE + " 分！";
            accent = hillLeader == null ? new ColorRGBA(1f, 0.84f, 0.46f, 1f)
                    : factionColor(hillLeader);
        } else {
            title = "冒險結束";
            subtitle = "生命值已耗盡，整備後再挑戰一次吧。";
            accent = new ColorRGBA(1f, 0.48f, 0.43f, 1f);
        }

        graphics.setFont(new Font(UI_FONT, Font.BOLD, 38));
        graphics.setColor(toAwtColor(accent));
        drawCentered(graphics, title, width, panelY + 92);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 18));
        graphics.setColor(new java.awt.Color(218, 228, 239));
        drawCentered(graphics, subtitle, width, panelY + 136);

        if (state == GameStateManager.State.VICTORY) {
            graphics.setFont(new Font(UI_FONT, Font.BOLD, 17));
            graphics.setColor(factionAwtColor(Faction.RED));
            drawCentered(graphics, "紅隊 " + factionScores.get(Faction.RED) + " 分", width, panelY + 185);
            graphics.setColor(factionAwtColor(Faction.GREEN));
            drawCentered(graphics, "綠隊 " + factionScores.get(Faction.GREEN) + " 分", width, panelY + 213);
            graphics.setColor(factionAwtColor(Faction.BLUE));
            drawCentered(graphics, "藍隊 " + factionScores.get(Faction.BLUE) + " 分", width, panelY + 241);
        } else if (state != GameStateManager.State.MAIN_MENU) {
            graphics.setFont(new Font(UI_FONT, Font.BOLD, 17));
            graphics.setColor(new java.awt.Color(255, 221, 145));
            drawCentered(graphics, "最終分數：" + score + "　|　擊敗怪物：" + defeatedMonsters,
                    width, panelY + 178);
        }

        int buttonWidth = Math.min(330, panelWidth - 64);
        int buttonHeight = 54;
        int buttonX = (width - buttonWidth) / 2;
        int firstButtonY = panelY + (state == GameStateManager.State.MAIN_MENU ? 190 : 270);
        if (state == GameStateManager.State.MAIN_MENU) {
            drawMenuButton(graphics, buttonX, firstButtonY, buttonWidth, buttonHeight,
                    "開始冒險", MenuAction.START, new java.awt.Color(55, 151, 108));
            drawMenuButton(graphics, buttonX, firstButtonY + 72, buttonWidth, buttonHeight,
                    "離開遊戲", MenuAction.EXIT, new java.awt.Color(75, 88, 105));
        } else {
            drawMenuButton(graphics, buttonX, firstButtonY, buttonWidth, buttonHeight,
                    "重新開始", MenuAction.RESTART, new java.awt.Color(55, 151, 108));
            drawMenuButton(graphics, buttonX, firstButtonY + 72, buttonWidth, buttonHeight,
                    "返回主選單", MenuAction.MAIN_MENU, new java.awt.Color(75, 88, 105));
        }

        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        graphics.setColor(new java.awt.Color(155, 171, 190));
        drawCentered(graphics, "WASD 移動 | E 探索 | Space 攻擊 | H 藥水 | Q 逃跑",
                width, panelY + panelHeight - 22);
    }

    private void drawMenuButton(Graphics2D graphics, int x, int y, int width, int height,
                                String label, MenuAction action, java.awt.Color color) {
        graphics.setColor(new java.awt.Color(0, 0, 0, 90));
        graphics.fillRoundRect(x, y + 4, width, height, 16, 16);
        graphics.setColor(color);
        graphics.fillRoundRect(x, y, width, height, 16, 16);
        graphics.setColor(new java.awt.Color(255, 255, 255, 110));
        graphics.setStroke(new BasicStroke(1.2f));
        graphics.drawRoundRect(x, y, width, height, 16, 16);
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 19));
        graphics.setColor(java.awt.Color.WHITE);
        drawCentered(graphics, label, width, y + 35, x);
        menuButtons.add(new MenuButton(x, y, width, height, action));
    }

    private void drawCentered(Graphics2D graphics, String text, int totalWidth, int baselineY) {
        drawCentered(graphics, text, totalWidth, baselineY, 0);
    }

    private void drawCentered(Graphics2D graphics, String text, int totalWidth,
                              int baselineY, int xOffset) {
        int x = xOffset + (totalWidth - graphics.getFontMetrics().stringWidth(text)) / 2;
        graphics.drawString(text, x, baselineY);
    }

    private java.awt.Color toAwtColor(ColorRGBA color) {
        return new java.awt.Color(Math.round(color.r * 255f),
                Math.round(color.g * 255f), Math.round(color.b * 255f));
    }

    private void drawControls(Graphics2D graphics, int width) {
        int panelWidth = Math.min(330, width - 48);
        int x = width - panelWidth - 24;
        drawPanel(graphics, x, 22, panelWidth, 190, new java.awt.Color(15, 24, 36, 205));
        graphics.setFont(new Font(UI_FONT, Font.BOLD, 16));
        graphics.setColor(new java.awt.Color(255, 221, 145));
        graphics.drawString("操作方式", x + 20, 50);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 14));
        graphics.setColor(new java.awt.Color(224, 232, 241));
        graphics.drawString("WASD / 方向鍵   移動", x + 20, 78);
        graphics.drawString("Shift                 加速", x + 20, 101);
        graphics.drawString("E                      探索 / 互動", x + 20, 124);
        graphics.drawString("Space / H / Q    攻擊 / 藥水 / 逃跑", x + 20, 147);
        graphics.drawString("M                      向商人購買", x + 20, 170);
        graphics.drawString("運輸兵依戰況自主選擇馬車路線", x + 20, 193);
    }

    private void drawMiniMap(Graphics2D graphics, int screenWidth) {
        int panelSize = 248;
        int panelX = screenWidth - panelSize - 24;
        int panelY = 220;
        drawPanel(graphics, panelX, panelY, panelSize, panelSize,
                new java.awt.Color(15, 24, 36, 220));

        graphics.setFont(new Font(UI_FONT, Font.BOLD, 16));
        graphics.setColor(new java.awt.Color(255, 221, 145));
        graphics.drawString("小地圖", panelX + 18, panelY + 25);
        graphics.setFont(new Font(UI_FONT, Font.PLAIN, 12));
        graphics.setColor(new java.awt.Color(206, 218, 231));
        graphics.drawString("紅 ●  綠 ●  藍 ●  黃 ●熱區", panelX + 18, panelY + 44);

        int mapX = panelX + 18;
        int mapY = panelY + 56;
        int mapSize = panelSize - 36;
        int centerX = mapX + mapSize / 2;
        int centerY = mapY + mapSize / 2;
        float mapRadius = WORLD_LIMIT;
        float scale = (mapSize * 0.5f - 8f) / mapRadius;

        java.awt.Shape previousClip = graphics.getClip();
        graphics.setClip(mapX, mapY, mapSize, mapSize);
        graphics.setColor(new java.awt.Color(34, 53, 61, 235));
        graphics.fillRect(mapX, mapY, mapSize, mapSize);
        graphics.setColor(new java.awt.Color(120, 146, 155, 150));
        graphics.setStroke(new BasicStroke(1f));
        graphics.drawRect(mapX, mapY, mapSize, mapSize);
        graphics.drawOval(centerX - mapSize / 3, centerY - mapSize / 3,
                2 * mapSize / 3, 2 * mapSize / 3);
        graphics.drawLine(centerX, mapY + 3, centerX, mapY + mapSize - 3);
        graphics.drawLine(mapX + 3, centerY, mapX + mapSize - 3, centerY);
        float areaRadius = CONTROL_AREA_RADIUS * scale;
        graphics.setColor(new java.awt.Color(255, 218, 120, 150));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawOval(centerX - Math.round(areaRadius), centerY - Math.round(areaRadius),
                Math.round(areaRadius * 2f), Math.round(areaRadius * 2f));
        for (Faction faction : Faction.values()) {
            Vector3f base = factionBases.get(faction);
            int baseX = centerX + Math.round(base.x * scale);
            int baseY = centerY + Math.round(base.z * scale);
            int baseRadius = Math.round(FACTION_BASE_RADIUS * scale);
            graphics.setColor(factionAwtColor(faction));
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawOval(baseX - baseRadius, baseY - baseRadius,
                    baseRadius * 2, baseRadius * 2);
            graphics.setFont(new Font(UI_FONT, Font.BOLD, 10));
            graphics.drawString(faction == Faction.RED ? "紅安全區"
                            : faction == Faction.GREEN ? "綠安全區" : "藍安全區",
                    baseX - 18, baseY - baseRadius - 3);
        }
        graphics.setColor(new java.awt.Color(255, 227, 128));
        for (Vector3f highGround : highGroundPositions) {
            int x = centerX + Math.round(highGround.x * scale);
            int y = centerY + Math.round(highGround.z * scale);
            int[] markerX = {x, x + 5, x, x - 5};
            int[] markerY = {y - 5, y, y + 5, y};
            graphics.fillPolygon(markerX, markerY, 4);
        }
        for (ForwardBase forwardBase : forwardBases.values()) {
            drawMapDot(graphics, centerX, centerY, mapSize, scale,
                    forwardBase.position.x, forwardBase.position.z,
                    factionAwtColor(forwardBase.faction), forwardBase.built ? 10 : 6);
        }
        for (TransportWagon wagon : transportWagons) {
            if (wagon.destination != null && wagon.phase != TransportPhase.BOARDING) {
                int startX = centerX + Math.round(wagon.position.x * scale);
                int startY = centerY + Math.round(wagon.position.z * scale);
                int endX = centerX + Math.round(wagon.destination.x * scale);
                int endY = centerY + Math.round(wagon.destination.z * scale);
                graphics.setColor(factionAwtColor(wagon.faction));
                graphics.setStroke(new BasicStroke(1f));
                graphics.drawLine(startX, startY, endX, endY);
            }
            drawMapDot(graphics, centerX, centerY, mapSize, scale,
                    wagon.position.x, wagon.position.z,
                    factionAwtColor(wagon.faction), 9);
        }

        Vector3f playerPosition = player.getLocalTranslation();
            for (MapEvent event : mapEvents) {
                drawMapDot(graphics, centerX, centerY, mapSize, scale,
                        event.position.x, event.position.z,
                        colorForEvent(event.type), 6);
            }
            Vector3f hotPosition = hotZoneNode.getLocalTranslation();
        drawMapDot(graphics, centerX, centerY, mapSize, scale,
                hotPosition.x, hotPosition.z,
                new java.awt.Color(255, 206, 48), 13);
        for (BattleActor actor : allBattleActors()) {
            if (actor.ridingWagon) {
                continue;
            }
            Vector3f position = actor.alive
                    ? actor.model.getLocalTranslation() : actor.initialPosition;
            java.awt.Color marker = actor.alive
                    ? factionAwtColor(actor.faction) : new java.awt.Color(150, 157, 166);
            drawMapDot(graphics, centerX, centerY, mapSize, scale,
                    position.x, position.z,
                    marker, actor.alive ? 6 : 4);
        }
        for (DefenseStructure structure : defenseStructures) {
            drawMapDot(graphics, centerX, centerY, mapSize, scale,
                    structure.position.x, structure.position.z,
                    factionAwtColor(structure.faction), 8);
        }
        for (BattleActor actor : allBattleActors()) {
            if (actor.building && actor.buildSite != null) {
                drawMapDot(graphics, centerX, centerY, mapSize, scale,
                        actor.buildSite.x, actor.buildSite.z,
                        new java.awt.Color(255, 224, 96), 8);
            }
        }
        if (monsterModel != null) {
            Vector3f position = monsterModel.getLocalTranslation();
            drawMapDot(graphics, centerX, centerY, mapSize, scale,
                    position.x, position.z,
                    new java.awt.Color(255, 83, 72), 8);
        }

        graphics.setColor(java.awt.Color.WHITE);
        double angle = Math.atan2(facingDirection.z, facingDirection.x);
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        int playerMapX = centerX + Math.round(playerPosition.x * scale);
        int playerMapY = centerY + Math.round(playerPosition.z * scale);
        for (int i = 0; i < 3; i++) {
            double pointAngle = angle + (i == 0 ? 0 : i == 1 ? 2.45 : -2.45);
            int radius = i == 0 ? 10 : 7;
            xPoints[i] = playerMapX + (int) (Math.cos(pointAngle) * radius);
            yPoints[i] = playerMapY + (int) (Math.sin(pointAngle) * radius);
        }
        graphics.fillPolygon(xPoints, yPoints, 3);
        graphics.setClip(previousClip);
    }

    private String jobCompositionText() {
        StringBuilder summary = new StringBuilder();
        for (JobType job : JobType.values()) {
            if (summary.length() > 0) {
                summary.append("　");
            }
            int count = 0;
            for (BattleActor actor : allBattleActors()) {
                if (actor.job == job) {
                    count++;
                }
            }
            summary.append(job.getDisplayName()).append(count);
        }
        return summary.toString();
    }

    private String constructionSummary() {
        StringBuilder summary = new StringBuilder();
        for (BattleActor actor : allBattleActors()) {
            if (!actor.alive || !actor.building) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append("、");
            }
            int progress = Math.round(100f * (1f - actor.buildTimer / actor.buildDuration));
            summary.append(actor.name).append("建造")
                    .append(actor.buildType.label).append(" ")
                    .append(progress).append("%");
        }
        return summary.length() == 0 ? "無" : summary.toString();
    }

    private String forwardBaseStatus(Faction faction) {
        ForwardBase forwardBase = forwardBases.get(faction);
        if (forwardBase == null) {
            return "待建";
        }
        if (forwardBase.built) {
            return "已建成";
        }
        if (forwardBase.building) {
            return "施工" + (int) Math.ceil(forwardBase.buildRemaining) + "秒";
        }
        return "建造中";
    }

    private String transportStatus(Faction faction) {
        StringBuilder status = new StringBuilder();
        for (TransportWagon wagon : transportWagons) {
            if (wagon.faction != faction) {
                continue;
            }
            if (status.length() > 0) {
                status.append("、");
            }
            if (wagon.phase == TransportPhase.BOARDING) {
                status.append("集合");
            } else if (wagon.phase == TransportPhase.OUTBOUND) {
                status.append(wagon.destinationMode.label).append("運送");
            } else if (wagon.phase == TransportPhase.RETURNING) {
                status.append("返回");
            } else {
                status.append("待命");
            }
            status.append(wagon.passengers.size()).append("/").append(TRANSPORT_CAPACITY);
        }
        return status.length() == 0 ? "待命" : status.toString();
    }

    private void drawMapDot(Graphics2D graphics, int centerX, int centerY, int mapSize,
                            float scale, float worldX, float worldZ,
                            java.awt.Color color, int dotSize) {
        int x = centerX + Math.round(worldX * scale);
        int y = centerY + Math.round(worldZ * scale);
        int halfMap = mapSize / 2;
        if (x < centerX - halfMap || x > centerX + halfMap
                || y < centerY - halfMap || y > centerY + halfMap) {
            return;
        }
        graphics.setColor(new java.awt.Color(0, 0, 0, 130));
        graphics.fillOval(x - dotSize / 2 - 1, y - dotSize / 2 - 1,
                dotSize + 2, dotSize + 2);
        graphics.setColor(color);
        graphics.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
    }

    private java.awt.Color colorForEvent(EventType type) {
        if (type == EventType.MONSTER) {
            return new java.awt.Color(255, 83, 72);
        }
        if (type == EventType.TREASURE) {
            return new java.awt.Color(255, 207, 69);
        }
        if (type == EventType.TRAP) {
            return new java.awt.Color(227, 103, 232);
        }
        return new java.awt.Color(67, 204, 231);
    }

    private void drawPanel(Graphics2D graphics, int x, int y, int width, int height,
                           java.awt.Color color) {
        graphics.setColor(new java.awt.Color(0, 0, 0, 85));
        graphics.fillRoundRect(x, y + 5, width, height, 22, 22);
        graphics.setColor(color);
        graphics.fillRoundRect(x, y, width, height, 22, 22);
        graphics.setColor(new java.awt.Color(255, 255, 255, 45));
        graphics.setStroke(new BasicStroke(1f));
        graphics.drawRoundRect(x, y, width, height, 22, 22);
    }

    private List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            if (line.length() > 0 && metrics.stringWidth(line.toString() + character) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
            offset += Character.charCount(codePoint);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void explore() {
        if (!adventurer.isAlive()) {
            log("冒險已結束，無法繼續探索。");
            return;
        }
        if (currentMonster != null) {
            log("戰鬥中！按 Space 攻擊、H 使用藥水，或 Q 嘗試逃跑。");
            return;
        }
        if (merchantAvailable) {
            merchantAvailable = false;
            log("你向商人道別，繼續踏上旅程。");
            spawnMapEvent();
            updateNearbyEvent();
            return;
        }

        updateNearbyEvent();
        if (nearbyEvent == null) {
            log("附近沒有探索點。沿著地圖尋找發光的事件標記，靠近後按 E 探索。");
            return;
        }
        MapEvent event = nearbyEvent;
        mapEvents.remove(event);
        event.node.removeFromParent();
        nearbyEvent = null;
        triggerEvent(event.type);
        updateNearbyEvent();
    }

    private void triggerEvent(EventType type) {
        merchantAvailable = false;
        if (type == EventType.MONSTER) {
            currentMonster = Monster.randomMonster(random);
            spawnMonster();
            log("你探索時遭遇 " + currentMonster.getName() + "！按 Space 攻擊或 Q 逃跑。");
        } else if (type == EventType.TREASURE) {
            log("你打開了藏在這裡的寶箱。");
            findTreasure();
            spawnMapEvent();
        } else if (type == EventType.TRAP) {
            int damage = 8 + random.nextInt(13);
            adventurer.takeDamage(damage);
            log("你踏入陷阱，受到 " + damage + " 點傷害。");
            checkDefeat();
            spawnMapEvent();
        } else {
            merchantAvailable = true;
            log("你找到一位旅行商人！按 M 花 " + MERCHANT_PRICE
                    + " 枚金幣購買生命藥水，或再按 E 離開。");
        }
    }

    private void findTreasure() {
        playRewardSound();
        if (random.nextBoolean()) {
            adventurer.addPotions(1);
            log("你在寶箱裡找到一瓶生命藥水！");
            return;
        }

        String[] prefixes = {"", "鋒利的", "精良的", "傳說之"};
        int[] bonuses = {0, 5, 10, 18};
        int tier = random.nextInt(prefixes.length);
        String weapon = prefixes[tier] + "長劍";
        if (adventurer.equipWeapon(weapon, bonuses[tier])) {
            log("寶箱中找到並裝備了「" + weapon + "」，攻擊力 +" + bonuses[tier] + "！");
        } else {
            adventurer.addCoins(2);
            log("寶箱裡的「" + weapon + "」不如你目前的武器，你將它賣掉，獲得 2 枚金幣。");
        }
    }

    private void spawnMonster() {
        monsterModel = new Node("Encountered monster");
        Geometry body = new Geometry("Monster body", new Sphere(16, 16, 1f));
        body.setMaterial(litMaterial(new ColorRGBA(0.72f, 0.14f, 0.12f, 1f)));
        body.setLocalScale(0.85f, 1.05f, 0.75f);
        body.setLocalTranslation(0f, 1f, 0f);
        monsterModel.attachChild(body);

        Geometry eyes = new Geometry("Monster eyes", new Box(0.38f, 0.1f, 0.08f));
        eyes.setMaterial(litMaterial(new ColorRGBA(1f, 0.85f, 0.2f, 1f)));
        eyes.setLocalTranslation(0f, 1.3f, 0.68f);
        monsterModel.attachChild(eyes);
        monsterModel.setLocalTranslation(player.getLocalTranslation()
                .add(facingDirection.mult(2.4f)));
        rootNode.attachChild(monsterModel);
    }

    private void populateMapEvents() {
        List<EventType> types = new ArrayList<>();
        EventType[] availableTypes = EventType.values();
        for (int i = 0; i < MAP_EVENT_COUNT; i++) {
            types.add(availableTypes[i % availableTypes.length]);
        }
        Collections.shuffle(types, random);
        for (EventType type : types) {
            spawnMapEvent(type);
        }
        log("地圖上已散布 " + MAP_EVENT_COUNT + " 個探索點；靠近標記後按 E 互動。");
    }

    private void spawnMapEvent() {
        EventType[] types = EventType.values();
        spawnMapEvent(types[random.nextInt(types.length)]);
    }

    private void spawnMapEvent(EventType type) {
        Vector3f position = findEventPosition();
        Node marker = createEventMarker(type);
        marker.setLocalTranslation(position);
        rootNode.attachChild(marker);
        mapEvents.add(new MapEvent(type, marker, position));
    }

    private Vector3f findEventPosition() {
        for (int attempt = 0; attempt < 80; attempt++) {
            float x = random.nextFloat() * 920f - 460f;
            float z = random.nextFloat() * 920f - 460f;
            Vector3f candidate = new Vector3f(x, 0f, z);
            if (candidate.distance(player.getLocalTranslation()) < 14f
                    || (FastMath.abs(x) < 4f && FastMath.abs(z) < 8f)) {
                continue;
            }
            boolean tooClose = false;
            for (MapEvent event : mapEvents) {
                if (candidate.distance(event.position) < 8f) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                return candidate;
            }
        }
        return new Vector3f(random.nextFloat() * 900f - 450f, 0f,
                random.nextFloat() * 900f - 450f);
    }

    private Node createEventMarker(EventType type) {
        Node marker = new Node(type.name() + " exploration point");
        Geometry base;
        if (type == EventType.MONSTER) {
            base = new Geometry("Monster marker", new Sphere(12, 12, 0.8f));
            base.setMaterial(litMaterial(new ColorRGBA(0.9f, 0.12f, 0.1f, 1f)));
            base.setLocalTranslation(0f, 1f, 0f);
        } else if (type == EventType.TREASURE) {
            base = new Geometry("Treasure marker", new Box(0.65f, 0.5f, 0.55f));
            base.setMaterial(litMaterial(new ColorRGBA(0.95f, 0.66f, 0.08f, 1f)));
            base.setLocalTranslation(0f, 0.55f, 0f);
        } else if (type == EventType.TRAP) {
            base = new Geometry("Trap marker", new Cylinder(16, 16, 0.9f, 0.14f));
            base.setMaterial(litMaterial(new ColorRGBA(0.78f, 0.12f, 0.66f, 1f)));
            base.setLocalTranslation(0f, 0.12f, 0f);
        } else {
            base = new Geometry("Merchant marker", new Cylinder(10, 10, 0.48f, 1.4f));
            base.setMaterial(litMaterial(new ColorRGBA(0.12f, 0.55f, 0.95f, 1f)));
            base.setLocalTranslation(0f, 0.8f, 0f);
            Geometry head = new Geometry("Merchant head", new Sphere(10, 10, 0.35f));
            head.setMaterial(litMaterial(new ColorRGBA(0.94f, 0.72f, 0.5f, 1f)));
            head.setLocalTranslation(0f, 1.7f, 0f);
            marker.attachChild(head);
        }
        marker.attachChild(base);

        Geometry beacon = new Geometry("Event beacon", new Cylinder(12, 12, 0.12f, 1.2f));
        beacon.setMaterial(unshadedMaterial(new ColorRGBA(1f, 0.94f, 0.45f, 0.72f)));
        beacon.setLocalTranslation(0f, 2.05f, 0f);
        marker.attachChild(beacon);
        return marker;
    }

    private void updateNearbyEvent() {
        MapEvent closest = null;
        float closestDistance = INTERACTION_DISTANCE;
        Vector3f playerPosition = player.getLocalTranslation();
        for (MapEvent event : mapEvents) {
            float distance = playerPosition.distance(event.position);
            if (distance <= closestDistance) {
                closest = event;
                closestDistance = distance;
            }
        }
        nearbyEvent = closest;
        String nextPrompt;
        ColorRGBA nextColor;
        if (nearbyEvent != null && currentMonster == null && adventurer.isAlive()) {
            nextPrompt = "按 E 探索：" + nearbyEvent.type.label;
            nextColor = nearbyEvent.type.color;
        } else if (merchantAvailable) {
            nextPrompt = "按 M 購買藥水，或按 E 離開商人";
            nextColor = ColorRGBA.Cyan;
        } else if (currentMonster != null) {
            nextPrompt = "戰鬥中：Space 攻擊 | H 藥水 | Q 逃跑";
            nextColor = ColorRGBA.Red;
        } else if (!adventurer.isAlive()) {
            nextPrompt = "冒險結束";
            nextColor = ColorRGBA.Gray;
        } else {
            nextPrompt = "尋找地圖上的彩色探索標記，靠近後按 E";
            nextColor = ColorRGBA.White;
        }
        if (!nextPrompt.equals(promptMessage) || !nextColor.equals(promptColor)) {
            promptMessage = nextPrompt;
            promptColor = nextColor;
            renderHud();
        }
    }

    private void attack() {
        if (!adventurer.isAlive()) {
            return;
        }
        BattleActor targetActor = nearestEnemy(player.getLocalTranslation(), 4f);
        if (targetActor != null) {
            attackAnimationTime = 0.42f;
            playAttackSound();
            if (createWeaponHitbox().intersects(createActorHitbox(targetActor))) {
                damageActor(targetActor, adventurer.attack());
                log("你擊中 " + targetActor.name + "！");
            } else {
                log("揮擊落空！面向紅隊敵人並靠近後再攻擊。");
            }
            updateHud();
            return;
        }
        if (currentMonster == null) {
            log("目前沒有可攻擊的怪物。");
            return;
        }

        attackAnimationTime = 0.42f;
        playAttackSound();
        BoundingBox weaponHitbox = createWeaponHitbox();
        BoundingBox monsterHitbox = createMonsterHitbox();
        if (!weaponHitbox.intersects(monsterHitbox)) {
            log("揮擊落空！請面向怪物並靠近後再攻擊。");
            updateHud();
            return;
        }

        int damage = adventurer.attack();
        currentMonster.takeDamage(damage);
        playHitSound();
        log("你攻擊 " + currentMonster.getName() + "，造成 " + damage + " 點傷害。");
        if (!currentMonster.isAlive()) {
            log("你擊敗了 " + currentMonster.getName() + "，獲得 3 枚金幣！");
            defeatedMonsters++;
            score += 100;
            adventurer.addCoins(3);
            playRewardSound();
            endBattle();
            spawnMapEvent();
        } else {
            monsterTurn();
        }
        updateHud();
    }

    private void usePotion() {
        int restored = adventurer.usePotion();
        if (restored == 0) {
            log(adventurer.getPotionCount() == 0
                    ? "背包裡沒有生命藥水！"
                    : "生命值已滿，不需要使用藥水。");
            return;
        }
        log("你使用生命藥水，恢復 " + restored + " 點生命值。");
        playHealSound();
        if (currentMonster != null) {
            monsterTurn();
        }
        updateHud();
    }

    private void flee() {
        if (currentMonster == null) {
            log("目前沒有戰鬥，不需要逃跑。");
            return;
        }
        if (random.nextBoolean()) {
            log("你成功逃離了戰鬥。");
            endBattle();
            spawnMapEvent();
        } else {
            log("逃跑失敗！");
            monsterTurn();
        }
        updateHud();
    }

    private void monsterTurn() {
        int damage = currentMonster.attack();
        adventurer.takeDamage(damage);
        playHitSound();
        log(currentMonster.getName() + " 對你造成 " + damage + " 點傷害。");
        checkDefeat();
    }

    private void checkDefeat() {
        if (!adventurer.isAlive() && playerRespawnTimer <= 0f) {
            playerRespawnTimer = PLAYER_RESPAWN_SECONDS;
            player.setCullHint(Spatial.CullHint.Always);
            playerKnockback.set(0f, 0f, 0f);
            playerStunTimer = 0f;
            endBattle();
            log("你已陣亡，" + PLAYER_RESPAWN_SECONDS + " 秒後會在藍隊安全區重生。");
            updateHud();
        }
    }

    private void respawnPlayer() {
        adventurer.revive();
        player.setLocalTranslation(factionBases.get(Faction.BLUE));
        player.setCullHint(Spatial.CullHint.Inherit);
        playerKnockback.set(0f, 0f, 0f);
        playerStunTimer = 0f;
        playerRespawnTimer = 0f;
        playerSpawnProtectionTimer = PLAYER_RESPAWN_PROTECTION_SECONDS;
        facingDirection.set(0f, 0f, 1f);
        log("你已在藍隊邊緣安全區重生，短時間內不會受到傷害。");
        updateHud();
    }

    private void endBattle() {
        if (monsterModel != null) {
            monsterModel.removeFromParent();
            monsterModel = null;
        }
        currentMonster = null;
    }

    private void buyPotion() {
        if (!merchantAvailable) {
            log("附近沒有商人。先按 E 探索。");
            return;
        }
        if (!adventurer.spendCoins(MERCHANT_PRICE)) {
            log("金幣不足；生命藥水需要 " + MERCHANT_PRICE + " 枚，你目前有 "
                    + adventurer.getCoins() + " 枚。");
            return;
        }
        adventurer.addPotions(1);
        playRewardSound();
        merchantAvailable = false;
        log("你買下了一瓶生命藥水。");
        spawnMapEvent();
        updateHud();
        updateNearbyEvent();
    }

    private void updateHud() {
        renderHud();
    }

    private void log(String message) {
        messages.add(message);
        while (messages.size() > 4) {
            messages.remove(0);
        }
        renderHud();
    }

    private Material litMaterial(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.65f));
        material.setColor("Specular", ColorRGBA.White.mult(0.12f));
        material.setFloat("Shininess", 8f);
        return material;
    }

    private Material unshadedMaterial(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }

    private enum EventType {
        MONSTER("怪物", new ColorRGBA(1f, 0.35f, 0.25f, 1f)),
        TREASURE("寶箱", new ColorRGBA(1f, 0.82f, 0.15f, 1f)),
        TRAP("陷阱", new ColorRGBA(1f, 0.45f, 0.9f, 1f)),
        MERCHANT("商人", new ColorRGBA(0.35f, 0.8f, 1f, 1f));

        private final String label;
        private final ColorRGBA color;

        EventType(String label, ColorRGBA color) {
            this.label = label;
            this.color = color;
        }
    }

    private enum DefenseType {
        WALL("防禦牆"),
        ARCHER_TOWER("弓箭塔"),
        TURRET("炮塔");

        private final String label;

        DefenseType(String label) {
            this.label = label;
        }
    }

    private enum TransportPhase {
        BOARDING,
        OUTBOUND,
        RETURNING,
        IDLE
    }

    private enum TransportDestination {
        HOT_ZONE("熱區"),
        FORWARD_BASE("FOB"),
        ENEMY_REAR("敵方後方");

        private final String label;

        TransportDestination(String label) {
            this.label = label;
        }
    }

    private enum LocomotionState {
        IDLE,
        RUNNING
    }

    private enum MenuAction {
        START,
        RESTART,
        MAIN_MENU,
        EXIT
    }

    private static final class MenuButton {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final MenuAction action;

        private MenuButton(int x, int y, int width, int height, MenuAction action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }

        private boolean contains(float pointX, float pointY) {
            return pointX >= x && pointX <= x + width
                    && pointY >= y && pointY <= y + height;
        }
    }

    private static final class MapEvent {
        private final EventType type;
        private final Node node;
        private final Vector3f position;

        private MapEvent(EventType type, Node node, Vector3f position) {
            this.type = type;
            this.node = node;
            this.position = position.clone();
        }
    }

    private static final class WorldObstacle {
        private final float x;
        private final float z;
        private final float centerY;
        private final float radius;
        private final float height;

        private WorldObstacle(float x, float z, float centerY, float radius, float height) {
            this.x = x;
            this.z = z;
            this.centerY = centerY;
            this.radius = radius;
            this.height = height;
        }
    }

    private static final class Decoy {
        private final Node node;
        private final Vector3f position;
        private final Faction faction;
        private float life;

        private Decoy(Node node, Vector3f position, Faction faction, float life) {
            this.node = node;
            this.position = position.clone();
            this.faction = faction;
            this.life = life;
        }
    }

    private static final class BattleEffect {
        private final Node node;
        private final Geometry geometry;
        private final BitmapText label;
        private final Vector3f position;
        private final ColorRGBA color;
        private final float radius;
        private final float duration;
        private float life;

        private BattleEffect(Node node, Geometry geometry, BitmapText label,
                             Vector3f position, ColorRGBA color, float radius, float duration) {
            this.node = node;
            this.geometry = geometry;
            this.label = label;
            this.position = position.clone();
            this.color = color;
            this.radius = radius;
            this.duration = duration;
            this.life = duration;
        }
    }

    private final class DefenseStructure {
        private final Faction faction;
        private final DefenseType type;
        private final Node model;
        private final Vector3f position;
        private final BattleActor builder;
        private float attackTimer = 1f;
        private WorldObstacle wallObstacle;

        private DefenseStructure(Faction faction, DefenseType type, Node model,
                                 Vector3f position, BattleActor builder) {
            this.faction = faction;
            this.type = type;
            this.model = model;
            this.position = position.clone();
            this.builder = builder;
        }
    }

    private final class ForwardBase {
        private final Faction faction;
        private final Vector3f position;
        private BattleActor builder;
        private Node marker;
        private Node model;
        private float buildRemaining = FOB_BUILD_SECONDS;
        private boolean building;
        private boolean built;

        private ForwardBase(Faction faction, Vector3f position) {
            this.faction = faction;
            this.position = position.clone();
        }
    }

    private final class TransportWagon {
        private final Faction faction;
        private final BattleActor driver;
        private final Node model;
        private final Vector3f pickup;
        private final Vector3f position;
        private final List<BattleActor> boardingCandidates = new ArrayList<>();
        private final List<BattleActor> passengers = new ArrayList<>();
        private final int routePreference;
        private TransportPhase phase = TransportPhase.BOARDING;
        private TransportDestination destinationMode = TransportDestination.HOT_ZONE;
        private Vector3f destination;
        private float boardingTimer = TRANSPORT_BOARDING_SECONDS;
        private float idleTimer;
        private boolean boardingAnnouncementPending = true;

        private TransportWagon(Faction faction, BattleActor driver, Node model,
                               Vector3f pickup, int routePreference) {
            this.faction = faction;
            this.driver = driver;
            this.model = model;
            this.pickup = pickup.clone();
            this.position = pickup.clone();
            this.routePreference = routePreference % TransportDestination.values().length;
        }
    }

    private final class Projectile {
        private final Node model;
        private final BattleActor owner;
        private final BattleActor target;
        private final boolean targetPlayer;
        private final Vector3f velocity;
        private final Vector3f sourcePosition;
        private final int damage;
        private final float blastRadius;
        private final Faction faction;
        private float life = 3f;

        private Projectile(Node model, BattleActor owner, BattleActor target,
                           boolean targetPlayer, Vector3f velocity,
                           Vector3f sourcePosition, int damage, float blastRadius,
                           Faction faction) {
            this.model = model;
            this.owner = owner;
            this.target = target;
            this.targetPlayer = targetPlayer;
            this.velocity = velocity;
            this.sourcePosition = sourcePosition.clone();
            this.damage = damage;
            this.blastRadius = blastRadius;
            this.faction = faction;
        }
    }

    private final class BattleActor {
        private final String name;
        private final Faction faction;
        private final JobType job;
        private final Node model;
        private final Node torso;
        private final Node leftArm;
        private final Node rightArm;
        private final Node leftLeg;
        private final Node rightLeg;
        private final Vector3f initialPosition;
        private int health;
        private boolean alive = true;
        private boolean ridingWagon;
        private TransportWagon transportWagon;
        private float attackTimer;
        private float respawnTimer;
        private float stunTimer;
        private final Vector3f knockbackVelocity = new Vector3f();
        private final Vector3f lastMovePosition = new Vector3f();
        private final Vector3f lastMoveTarget = new Vector3f();
        private final Vector3f recoveryDirection = new Vector3f();
        private final Vector3f navigationWaypoint = new Vector3f();
        private boolean hasNavigationWaypoint;
        private float moveCheckTimer;
        private float stuckTimer;
        private float recoveryTimer;
        private float blockedMoveTimer;
        private float recoverySide = 1f;
        private float animationOffset;
        private float formationSide;
        private float formationBack;
        private float supportTimer;
        private float blinkCooldown;
        private int buildIndex;
        private float buildCooldown;
        private float buildTimer;
        private float buildDuration;
        private boolean building;
        private DefenseType buildType;
        private Vector3f buildSite;
        private Node buildMarker;

        private BattleActor(String name, Faction faction, JobType job, Node model, Node torso,
                            Node leftArm, Node rightArm, Node leftLeg, Node rightLeg,
                            Vector3f initialPosition) {
            this.name = name;
            this.faction = faction;
            this.job = job;
            this.model = model;
            this.torso = torso;
            this.leftArm = leftArm;
            this.rightArm = rightArm;
            this.leftLeg = leftLeg;
            this.rightLeg = rightLeg;
            this.initialPosition = initialPosition.clone();
            health = job.getMaxHealth();
        }

        private void reset() {
            if (buildMarker != null) {
                buildMarker.removeFromParent();
                buildMarker = null;
            }
            health = job.getMaxHealth();
            alive = true;
            ridingWagon = false;
            transportWagon = null;
            attackTimer = 0.3f;
            respawnTimer = 0f;
            stunTimer = 0f;
            knockbackVelocity.set(0f, 0f, 0f);
            lastMovePosition.set(initialPosition);
            lastMoveTarget.set(0f, 0f, 0f);
            recoveryDirection.set(0f, 0f, 0f);
            navigationWaypoint.set(0f, 0f, 0f);
            hasNavigationWaypoint = false;
            moveCheckTimer = 0f;
            stuckTimer = 0f;
            recoveryTimer = 0f;
            blockedMoveTimer = 0f;
            recoverySide = random.nextBoolean() ? 1f : -1f;
            supportTimer = 1f;
            blinkCooldown = 2f;
            buildIndex = 0;
            buildCooldown = 0f;
            buildTimer = 0f;
            buildDuration = 0f;
            building = false;
            buildType = null;
            buildSite = null;
            animationOffset = random.nextFloat() * FastMath.TWO_PI;
            model.setCullHint(Spatial.CullHint.Inherit);
            if (model.getParent() == null) {
                rootNode.attachChild(model);
            }
        }
    }
}
