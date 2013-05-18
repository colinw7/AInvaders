package colinw.org.space_invaders;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.View;

public class SpaceInvadersView extends View {
  private class ViewTimerTask extends TimerTask {
    public ViewTimerTask(SpaceInvadersView view) {
      view_ = view;
    }

    @Override
    public void run() {
      view_.update();

      view_.postInvalidate();
    }

    private SpaceInvadersView view_;
  }

  private class Point {
    Point() {
      x = 0;
      y = 0;
    }

    Point(int x1, int y1) {
      x = x1;
      y = y1;
    }

    public int x, y;
  }

  private class Rect {
    Rect(int x1_, int y1_, int x2_, int y2_) {
     x1 = x1_;
     y1 = y1_;
     x2 = x2_;
     y2 = y2_;
    }

    boolean overlaps(Rect rect) {
      if (x2 < rect.x1 || x1 > rect.x2 || y2 < rect.y1 || y1 > rect.y2)
        return false;

      return true;
    }

    public int x1, y1, x2, y2;
  }

  private class ImageList {
    ImageList() {
      ind_    = 0;
      images_ = new Vector<Bitmap>();
    }

    void addImage(Bitmap i) { images_.add(i); }

    void next() { ++ind_; if (ind_ >= images_.size()) ind_ = 0; }

    void reset() { ind_ = 0; }

    void draw(Canvas c, Point p) {
      if (! images_.isEmpty())
        c.drawBitmap(images_.get(ind_), p.x, p.y, null);
    }

    private int            ind_;
    private Vector<Bitmap> images_;
  };

  class Graphic {
    Graphic(Point pos, int w, int h) {
      pos_    = pos;
      images_ = new ImageList();
      w_      = w;
      h_      = h;
      dead_   = false;
    }

    Point getPos() { return pos_; }

    void addImage(Bitmap i) { images_.addImage(i); }

    void nextImage() { images_.next(); }

    void draw(Canvas c) {
      if (isDead()) return;

      Point pos = new Point(pos_.x - w_/2, pos_.y - h_/2);

      images_.draw(c, pos);
    }

    void reset() { dead_ = false; }

    boolean isDead() { return dead_; }

    void setDead(boolean dead) { dead_ = dead; }

    Rect rect() { return new Rect(pos_.x - w_/2, pos_.y - h_/2, pos_.x + w_/2, pos_.y + h_/2); }

    protected Point     pos_;
    protected ImageList images_;
    protected int       w_;
    protected int       h_;
    protected boolean   dead_;
  }

  class ExplodeGraphic extends Graphic {
    ExplodeGraphic(Point pos, int w, int h) {
      super(pos, w, h);

      exploding_     = 0;
      explodeImages_ = new ImageList();
    }

    boolean isExploding() { return exploding_ > 0; }

    void setExploding() { exploding_ = 4; }

    void reset() {
      super.reset();

      exploding_ = 0;
    }

    void draw(Canvas c) {
      if (isExploding())
        explodeImages_.draw(c, new Point(pos_.x - 24, pos_.y - 24));
      else
        super.draw(c);
    }

    void update() {
      if (isExploding()) {
        --exploding_;

        if (exploding_ == 0)
          setDead(true);

        return;
      }
    }

    protected int       exploding_;
    protected ImageList explodeImages_;
  };

  class Bullet extends Graphic {
    Bullet(Point pos, int w, int h) {
      super(pos, w, h);
    }

    void update() { }
  };

  class AlienBullet extends Bullet {
    private final int DY = 20;

    AlienBullet(Alien alien, Point pos) {
      super(pos, 9, 26);

      alien_ = alien;

      addImage(getBitmap(R.drawable.bullet2a));
    }

    void update() {
      pos_.y += DY;

      if (pos_.y >= SCREEN_HEIGHT)
        setDead(true);
    }

    private Alien alien_;
  };

  class PlayerBullet extends Bullet {
    private final int DY = 40;

    PlayerBullet(Player player, Point pos) {
      super(pos, 4, 26);

      player_ = player;

      addImage(getBitmap(R.drawable.bullet1a));
    }

    void update() {
      pos_.y -= DY;

      if (pos_.y < 10)
        setDead(true);
    }

    private Player player_;
  };

  class AlienManager {
    private final int NUM_BULLETS = 5;

    AlienManager() {
      dir_         = 1;
      speed_       = 8;
      w_           = 48;
      numAlive_    = 0;
      needsIncRow_ = false;
      fast_        = false;

      row_y_ = new int [5];

      for (int y = 0; y < 5; ++y)
        row_y_[y] = y*60 + 110;

      bullets_ = new AlienBullet [NUM_BULLETS];

      for (int i = 0; i < NUM_BULLETS; ++i)
        bullets_[i] = null;
    }

    void reset() {
      speed_ = 4 + 4*getLevel();

      for (int y = 0; y < 5; ++y)
        row_y_[y] = y*60 + 100;

      for (int i = 0; i < NUM_BULLETS; ++i)
        bullets_[i] = null;
      
      needsIncRow_ = false;
      
      fast_ = false;
    }

    int getWidth() { return w_; }

    int getRowY(int row) { return row_y_[row]; }

    int getDir() { return dir_; }

    int getSpeed() {
      if (! fast_)
        return speed_/4;
      else
    	return speed_/2;
    }

    void needsIncRow() { needsIncRow_ = true; }

    void preUpdate() {
      needsIncRow_ = false;

      numAlive_ = 0;
    }

    void postUpdate() {
      if (needsIncRow_) {
        for (int y = 0; y < 5; ++y)
          row_y_[y] += w_/2;

        dir_ = -dir_;

        ++speed_;

        needsIncRow_ = false;
      }

      if (numAlive_ == 0)
        nextLevel();
      
      fast_ = (numAlive_ < 4);
    }

    void update() {
      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] == null) continue;

        bullets_[i].update();

        if (! bullets_[i].isDead())
          checkPlayerHit(bullets_[i]);

        if (! bullets_[i].isDead())
          checkBaseHit(bullets_[i]);

        if (bullets_[i].isDead()) {
          bullets_[i] = null;
        }
      }
    }

    void fire(Alien alien) {
      Point pos = alien.getPos();

      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] != null) continue;

        bullets_[i] = new AlienBullet(alien, new Point(pos.x, pos.y + 24));

        return;
      }
    }

    void checkHit(PlayerBullet bullet) {
      if (bullet.isDead()) return;

      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] == null) continue;

        if (bullet.rect().overlaps(bullets_[i].rect())) {
          bullets_[i] = null;

          bullet.setDead(true);

          return;
        }
      }
    }

    void draw(Canvas c) {
      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] == null) continue;

        bullets_[i].draw(c);
      }
    }

    void incAlive() { ++numAlive_; }

    private int         row_y_[];
    private int         dir_;
    private int         speed_;
    private int         w_;
    private int         numAlive_;
    private boolean     needsIncRow_;
    private boolean     fast_;
    private AlienBullet bullets_[];
  };

  class Alien extends ExplodeGraphic {
    Alien(AlienManager mgr, int col, int row, Point pos, int w, int h) {
      super(pos, w, h);

      mgr_        = mgr;
      col_        = col;
      row_        = row;
      imageCount_ = 4;
      dieSound_   = getSound(R.raw.invaderkilled);

      explodeImages_.addImage(getBitmap(R.drawable.explode1));
    }

    int getScore() { return 0; }

    void reset() {
      super.reset();

      imageCount_ = 0;

      pos_.x = 34*(2*col_ + 1);
    }

    void checkHit(PlayerBullet bullet) {
      if (bullet.isDead()) return;

      if (isDead() || isExploding()) return;

      if (bullet.rect().overlaps(rect())) {
        setExploding();

        addScore(getScore());

        playSound(dieSound_);

        bullet.setDead(true);
      }
    }

    void update() {
      super.update();

      if (isDead()) return;

      pos_.x += mgr_.getSpeed()*mgr_.getDir();

      if (isExploding()) return;

      int hs = mgr_.getWidth()/2;

      if (pos_.x >= SCREEN_WIDTH - hs) {
        //pos_.x = SCREEN_WIDTH - hs - 1;

        mgr_.needsIncRow();
      }
      else if (pos_.x < hs) {
        //pos_.x = hs;

        mgr_.needsIncRow();
      }

      --imageCount_;

      if (imageCount_ <= 0) {
        nextImage();

        imageCount_ = 4;
      }

      if (random() < 0.005)
        mgr_.fire(this);

      mgr_.incAlive();

      int y = mgr_.getRowY(row_);

      if (y >  850) checkBaseHit(this);
      
      if (y > 1050) gameOver();
    }

    void draw(Canvas c) {
      pos_.y = mgr_.getRowY(row_);

      super.draw(c);
    }

    private AlienManager mgr_;
    private int          col_;
    private int          row_;
    private int          imageCount_;
    private int          dieSound_;
  };

  class Alien1 extends Alien {
    Alien1(AlienManager mgr, int col, int row, Point pos) {
      super(mgr, col, row, pos, 35, 35);

      addImage(getBitmap(R.drawable.invader1a));
      addImage(getBitmap(R.drawable.invader1b));
    }

    int getScore() { return 30; }
  };

  class Alien2 extends Alien {
    Alien2(AlienManager mgr, int col, int row, Point pos) {
      super(mgr, col, row, pos, 48, 35);

      addImage(getBitmap(R.drawable.invader2a));
      addImage(getBitmap(R.drawable.invader2b));
    }

    int getScore() { return 20; }
  };

  class Alien3 extends Alien {
    Alien3(AlienManager mgr, int col, int row, Point pos) {
      super(mgr, col, row, pos, 52, 35);

      addImage(getBitmap(R.drawable.invader3a));
      addImage(getBitmap(R.drawable.invader3b));
    }

    int getScore() { return 10; }
  };

  class MysteryAlien extends ExplodeGraphic {
    private final int DX = -4;

    MysteryAlien() {
      super(new Point(0, 60), 71, 31);

      addImage(getBitmap(R.drawable.mystery1a));

      explodeImages_.addImage(getBitmap(R.drawable.explode1));

      dieSound_ = getSound(R.raw.invaderkilled);

      dead_ = true;
    }

    int getScore() {
      double r = random();

      if      (r < 0.50) return 100;
      else if (r < 0.80) return 200;
      else if (r < 0.95) return 300;
      else               return 400;
    }

    void reset() {
      super.reset();

      dead_ = true;

      pos_.x = SCREEN_WIDTH + w_/2;
    }

    void update() {
      super.update();

      if (isDead()) return;

      pos_.x += DX;

      if (pos_.x < 0)
        setDead(true);
    }

    void checkHit(PlayerBullet bullet) {
      if (bullet.isDead()) return;

      if (isDead() || isExploding()) return;

      if (bullet.rect().overlaps(rect())) {
        setExploding();

        addScore(getScore());

        playSound(dieSound_);

        bullet.setDead(true);
      }
    }

    private int dieSound_;
  };

  class Score {
    Score(Point pos) {
      pos_   = pos;
      score_ = 0;
    }

    void add(int i) { score_ += i; }

    void draw(Canvas c) {
      String str = String.format("Score: %d", score_);

      drawCenteredText(c, pos_.x, pos_.y, str, text_paint);
    }

    void reset() {
      score_ = 0;
    }

    private Point pos_;
    private int   score_;
  };

  class Player extends Graphic {
    private final int DX          = 8;
    private final int NUM_LIVES   = 3;
    private final int NUM_BULLETS = 5;

    Player(Point pos) {
      super(pos, 57, 35);

      lives_      = NUM_LIVES;
      d_          = DX;
      fire_block_ = 0;

      addImage(getBitmap(R.drawable.player1a));

      bullets_ = new PlayerBullet [NUM_BULLETS];

      for (int i = 0; i < NUM_BULLETS; ++i)
        bullets_[i] = null;

      fireSound_ = getSound(R.raw.shoot);
      dieSound_  = getSound(R.raw.explosion);
    }

    void reset() {
      super.reset();

      pos_.x = SCREEN_WIDTH/2;
      
      lives_      = NUM_LIVES;
      fire_block_ = 0;

      for (int i = 0; i < NUM_BULLETS; ++i) {
        bullets_[i] = null;
      }
    }

    void moveLeft () {
      pos_.x -= d_;

      int hs = w_/2;

      if (pos_.x <  hs) pos_.x = hs;
    }

    void moveRight() {
      pos_.x += d_;

      int hs = w_/2;

      if (pos_.x >= SCREEN_WIDTH - hs) pos_.x = SCREEN_WIDTH - hs - 1;
    }

    void fire() {
      if (fire_block_ > 0) return;

      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] != null) continue;

        bullets_[i] = new PlayerBullet(this, new Point(pos_.x, pos_.y - h_/2));

        fire_block_ = 8;

        playSound(fireSound_);

        return;
      }
    }

    void update() {
      if (fire_block_ > 0) --fire_block_;

      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] == null) continue;

        bullets_[i].update();

        if (! bullets_[i].isDead())
          checkAlienHit(bullets_[i]);

        if (! bullets_[i].isDead())
          checkBaseHit(bullets_[i]);

        if (bullets_[i].isDead()) {
          bullets_[i] = null;
        }
      }
    }

    void checkHit(AlienBullet bullet) {
      if (bullet.isDead()) return;

      if (isDead()) return;

      if (bullet.rect().overlaps(rect())) {
        --lives_;

        playSound(dieSound_);

        bullet.setDead(true);

        if (lives_ <= 0)
          gameOver();
      }
    }

    void draw(Canvas c) {
      super.draw(c);

      for (int i = 0; i < NUM_BULLETS; ++i) {
        if (bullets_[i] == null) continue;

        bullets_[i].draw(c);
      }

      String str = String.format("Lives: %d", lives_);

      drawLeftText(c, 10, 10, str, text_paint);
    }

    private int          lives_;
    private int          d_;
    private int          fire_block_;
    private PlayerBullet bullets_[];
    private int          fireSound_;
    private int          dieSound_;
  };

  class Base extends Graphic {
    class Cell {
      ImageList images;
      int       ind;
      boolean   dead;
      Rect      rect;

      Cell(Rect r) {
        images = new ImageList();
        ind    = 0;
        dead   = false;
        rect   = r;
      }

      Rect getRect() { return rect; }

      void hit() { images.next(); ++ind; if (ind >= 4) dead = true; }

      void reset() { images.reset(); ind = 0; dead = false; }

      void addImage(Bitmap image) { images.addImage(image); }
    };

    Base(Point pos) {
      super(pos, 87, 57);

      grid_ = new Cell [2][4];

      int dx = 22;
      int dy = 29;

      int y1 = pos_.y + h_/2;
      int y2 = y1;

      for (int r = 0; r < 2; ++r) {
        y1 = y2;
        y2 = y1 + dy;

        int x1 = pos_.x - w_/2;
        int x2 = x1;

        for (int c = 0; c < 4; ++c) {
          x1 = x2;
          x2 = x1 + dx;

          grid_[r][c] = new Cell(new Rect(x1, y1, x2, y2));
        }
      }
      
      grid_[0][0].addImage(getBitmap(R.drawable.base1a_1_1));
      grid_[0][1].addImage(getBitmap(R.drawable.base1a_2_1));
      grid_[0][2].addImage(getBitmap(R.drawable.base1a_3_1));
      grid_[0][3].addImage(getBitmap(R.drawable.base1a_4_1));
      grid_[1][0].addImage(getBitmap(R.drawable.base1a_1_2));
      grid_[1][1].addImage(getBitmap(R.drawable.base1a_2_2));
      grid_[1][2].addImage(getBitmap(R.drawable.base1a_3_2));
      grid_[1][3].addImage(getBitmap(R.drawable.base1a_4_2));

      grid_[0][0].addImage(getBitmap(R.drawable.base1a_1_1));
      grid_[0][1].addImage(getBitmap(R.drawable.base1b_2_1));
      grid_[0][2].addImage(getBitmap(R.drawable.base1b_3_1));
      grid_[0][3].addImage(getBitmap(R.drawable.base1b_4_1));
      grid_[1][0].addImage(getBitmap(R.drawable.base1b_1_2));
      grid_[1][1].addImage(getBitmap(R.drawable.base1b_2_2));
      grid_[1][2].addImage(getBitmap(R.drawable.base1b_3_2));
      grid_[1][3].addImage(getBitmap(R.drawable.base1b_4_2));

      grid_[0][0].addImage(getBitmap(R.drawable.base1c_1_1));
      grid_[0][1].addImage(getBitmap(R.drawable.base1c_2_1));
      grid_[0][2].addImage(getBitmap(R.drawable.base1c_3_1));
      grid_[0][3].addImage(getBitmap(R.drawable.base1c_4_1));
      grid_[1][0].addImage(getBitmap(R.drawable.base1c_1_2));
      grid_[1][1].addImage(getBitmap(R.drawable.base1c_2_2));
      grid_[1][2].addImage(getBitmap(R.drawable.base1c_3_2));
      grid_[1][3].addImage(getBitmap(R.drawable.base1c_4_2));

      grid_[0][0].addImage(getBitmap(R.drawable.base1d_1_1));
      grid_[0][1].addImage(getBitmap(R.drawable.base1d_2_1));
      grid_[0][2].addImage(getBitmap(R.drawable.base1d_3_1));
      grid_[0][3].addImage(getBitmap(R.drawable.base1d_4_1));
      grid_[1][0].addImage(getBitmap(R.drawable.base1d_1_2));
      grid_[1][1].addImage(getBitmap(R.drawable.base1d_2_2));
      grid_[1][2].addImage(getBitmap(R.drawable.base1d_3_2));
      grid_[1][3].addImage(getBitmap(R.drawable.base1d_4_2));
    }

    void reset() {
      for (int r = 0; r < 2; ++r)
        for (int c = 0; c < 4; ++c)
          grid_[r][c].reset();
    }

    void draw(Canvas canvas) {
      int dx = 22;
      int dy = 29;

      int x = pos_.x - w_/2;
      int y = pos_.y + h_/2;

      for (int r = 0; r < 2; ++r) {
        for (int c = 0; c < 4; ++c) {
          Cell cell = grid_[r][c];

          if (cell.dead) continue;

          cell.images.draw(canvas, new Point(x + c*dx, y + r*dy));
        }
      }
    }

    void checkHit(Bullet bullet) {
      Rect brect = bullet.rect();

      for (int r = 0; r < 2; ++r) {
        for (int c = 0; c < 4; ++c) {
          Cell cell = grid_[r][c];

          if (brect.overlaps(cell.rect)) {
            if (cell.dead) continue;

            cell.hit();

            bullet.setDead(true);
          }
        }
      }
    }

    void checkHit(Alien alien) {
      for (int r = 0; r < 2; ++r) {
        for (int c = 0; c < 4; ++c) {
          Cell cell = grid_[r][c];

          if (alien.rect().overlaps(cell.rect)) {
            if (cell.dead) continue;

            cell.dead = true;
          }
        }
      }
    }

    private Cell grid_[][];
  };

  class Level {
    Level() {
      value_ = 1;
    }

    void draw(Canvas c) {
      String str = String.format("Level: %d", value_);

      drawRightText(c, SCREEN_WIDTH - 10, 10, str, text_paint);
    }

    int value() { return value_; }
    
    void next() { value_++; }
    
    void reset() { value_ = 1; }

    private int value_;
  };

  SpaceInvadersView(SpaceInvadersActivity activity) {
    super(activity);

    activity_ = activity;

    rand_ = new Random();

    timer_ = new Timer();

    soundPool_ = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

    width_  = 100;
    height_ = 100;

    text_paint = new Paint();

    text_paint.setColor(white_);
    text_paint.setTextSize(28);

    pressX_  = 0;
    pressY_  = 0;
    moveX_   = 0;
    moveY_   = 0;
    pressed_ = false;

    init();

    timer_.schedule(new ViewTimerTask(this), 10, 100);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    width_  = w;
    height_ = h;

    super.onSizeChanged(w, h, oldw, oldh);
  }

  void init() {
    player_ = new Player(new Point(400, 1050));

    level_ = new Level();

    score_ = new Score(new Point(SCREEN_WIDTH/2, 10));

    alienMgr_ = new AlienManager();

    aliens_ = new Vector<Alien>();

    mysteryAlien_ = new MysteryAlien();

    bases_ = new Vector<Base>();

    titleScreen_ = true;
    paused_      = false;
    gameOver_    = false;

    for (int i = 0; i < 4; ++i)
      addBase(new Point(98*(2*i + 1), 940));

    for (int y = 0; y < 5; ++y) {
      for (int x = 0; x < 11; ++x) {
        addAlien(x, y, 34*(2*x + 1));
      }
    }

    titleImage_ = getBitmap(R.drawable.title);
  }

  @Override
  protected void onDraw(Canvas c) {
    if (titleScreen_) {
      c.drawBitmap(titleImage_, 0, 150, null);
      return;
    }
      
    level_.draw(c);

    score_.draw(c);

    player_.draw(c);

    for (Alien a : aliens_) a.draw(c);
    for (Base  b : bases_ ) b.draw(c);

    alienMgr_.draw(c);

    mysteryAlien_.draw(c);

    if (gameOver_)
      drawCenteredText(c, SCREEN_WIDTH/2, SCREEN_HEIGHT/2, "GAME OVER", text_paint);
  }

  void update() {
    if (titleScreen_) return;

    if (paused_ || gameOver_) return;

    player_.update();

    alienMgr_.preUpdate();

    for (Alien a : aliens_) a.update();

    alienMgr_.postUpdate();

    alienMgr_.update();

    mysteryAlien_.update();

    if (mysteryAlien_.isDead()) {
      if (random() < 0.002) {
        mysteryAlien_.reset();

        mysteryAlien_.setDead(false);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (titleScreen_) {
      titleScreen_ = false;

      restart();

      return true;
    }

    if (paused_) {
      paused_ = false;
      
      return true;
    }
    
    final int midX = width_ /2;
    final int midY = height_/2;

    int action = (event.getAction() & MotionEvent.ACTION_MASK);

    boolean left  = false; int leftId  = 0; int leftX  = 0; int leftY  = 0;
    boolean right = false; int rightId = 0; int rightX = 0; int rightY = 0;

    for (int i = 0; i < event.getPointerCount(); i++) {
      int mouseX = (int) event.getX(i);
      int mouseY = (int) event.getY(i);

      if      (mouseX <= midX) {
        left  = true; leftId  = i; leftX  = mouseX; leftY  = mouseY;
      }
      else if (mouseX >  midX && mouseY >= midY) {
        right = true; rightId = i; rightX = mouseX; rightY = mouseY;
      }
    }

    if (left) {
      if      (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
    	pressed_ = true;

        pressX_ = leftX;
        pressY_ = leftY;
      }
      else if (action == MotionEvent.ACTION_MOVE) {
    	moveX_ = leftX;
        moveY_ = leftY;
    	
        int dx = moveX_ - pressX_;

        if      (dx <= -2)
          moveShipLeft();
        else if (dx >=  2)
          moveShipRight();
      }
      else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
    	 pressed_ = false;
      }
    }
    if (right) {
      if      (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        shipFire();
      }
      else if (action == MotionEvent.ACTION_MOVE) {
      }
      else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
      }
    }

    return true;
  }

  public boolean onBackPressed() {
    if       (titleScreen_) {
      return false;
    }
    else if (gameOver_) {
      titleScreen_ = true;
    }
    else {
      if (! paused_)
        pause();
      else
    	titleScreen_ = true;
    }
    
    return true;
  }

  void addAlien(int x_ind, int y_ind, int x) {
    int y = alienMgr_.getRowY(y_ind);

    Point pos = new Point(x, y);

    if      (y_ind == 0              ) aliens_.add(new Alien1(alienMgr_, x_ind, y_ind, pos));
    else if (y_ind == 1 || y_ind == 2) aliens_.add(new Alien2(alienMgr_, x_ind, y_ind, pos));
    else if (y_ind == 3 || y_ind == 4) aliens_.add(new Alien3(alienMgr_, x_ind, y_ind, pos));
  }

  void addBase(Point pos) {
    bases_.add(new Base(pos));
  }

  void moveShipLeft() {
    if (titleScreen_ || paused_ || gameOver_) return;

    player_.moveLeft();
  }

  void moveShipRight() {
    if (titleScreen_ || paused_ || gameOver_) return;

    player_.moveRight();
  }

  void shipFire() {
    if (titleScreen_ || paused_ || gameOver_) return;

    player_.fire();
  }

  void checkAlienHit(PlayerBullet bullet) {
    for (Alien a : aliens_) a.checkHit(bullet);

    alienMgr_.checkHit(bullet);

    mysteryAlien_.checkHit(bullet);
  }

  void checkPlayerHit(AlienBullet bullet) {
    player_.checkHit(bullet);
  }

  void checkBaseHit(Bullet bullet) {
    for (Base b : bases_) b.checkHit(bullet);
  }

  void checkBaseHit(Alien alien) {
    for (Base b : bases_) b.checkHit(alien);
  }

  void addScore(int score) {
    score_.add(score);
  }

  void pause() {
    paused_ = ! paused_;
  }

  void gameOver() {
    gameOver_ = true;
  }

  int getLevel() {
	 return level_.value();
  }
  
  void nextLevel() {
    paused_   = false;
    gameOver_ = false;

    level_.next();
    
    for (Alien a : aliens_) a.reset();

    alienMgr_.reset();

    mysteryAlien_.reset();
  }

  void restart() {
    if (! paused_ && ! gameOver_) return;

    paused_   = false;
    gameOver_ = false;

    level_.reset();

    score_.reset();

    player_.reset();

    for (Alien a : aliens_) a.reset();
    for (Base  b : bases_ ) b.reset();

    alienMgr_.reset();
  }

  //--------------

  Bitmap getBitmap(int id) {
    Resources res = activity_.getResources();

    return BitmapFactory.decodeResource(res, id);
  }

  int getSound(int id) {
   return soundPool_.load(activity_, id, 1);
  }

  void playSound(int id) {
    soundPool_.play(id, 1, 1, 0, 0, 1);
  }

  void drawLeftText(Canvas c, int x, int y, String text, Paint paint) {
    //android.Graphics.Rect bounds = new android.Graphics.Rect();

    //paint.getTextBounds(text, 0, text.length(), bounds);

    c.drawText(text, x, y + 14, paint);
  }

  void drawCenteredText(Canvas c, int x, int y, String text, Paint paint) {
    android.graphics.Rect bounds = new android.graphics.Rect();

    paint.getTextBounds(text, 0, text.length(), bounds);

    c.drawText(text, x - bounds.width()/2, y + 14, paint);
  }

  void drawRightText(Canvas c, int x, int y, String text, Paint paint) {
    android.graphics.Rect bounds = new android.graphics.Rect();

    paint.getTextBounds(text, 0, text.length(), bounds);

    c.drawText(text, x - bounds.width(), y + 14, paint);
  }

  double random() {
    return rand_.nextDouble();
  }

  //--------------

  private int white_  = 0xFFFFFFFF;

  int SCREEN_WIDTH  = 800;
  int SCREEN_HEIGHT = 1100;

  SpaceInvadersActivity activity_;

  private Random rand_;

  private Timer timer_;

  private SoundPool soundPool_;

  private int width_, height_;

  Paint text_paint;

  private int      pressX_;
  private int      pressY_;
  private int      moveX_;
  private int      moveY_;
  private boolean  pressed_;

  private Player        player_;
  private Level         level_;
  private Score         score_;
  private AlienManager  alienMgr_;
  private Vector<Alien> aliens_;
  private MysteryAlien  mysteryAlien_;
  private Vector<Base>  bases_;
  private boolean       titleScreen_;
  private boolean       paused_;
  private boolean       gameOver_;

  private Bitmap titleImage_;
}
