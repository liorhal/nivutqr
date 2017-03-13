import datetime
from app import db


class User(db.Model):
    __tablename__ = 'nqr_user'

    user_id = db.Column(db.Integer, primary_key=True)
    login = db.Column(db.String(255), index=True, unique=True)
    password = db.Column(db.String(255))
    created_date = db.Column(db.DateTime, default=datetime.datetime.utcnow)
    phone = db.Column(db.String(50))
    first_name = db.Column(db.String(50))
    last_name = db.Column(db.String(50))
    subscription = db.Column(db.Integer)

    games = db.relationship('Game', backref='user', lazy='dynamic')

    def __repr__(self):
        return self.login

    def __init__(self , login ,password):
        self.login = login
        self.password = password
        self.subscription = 3

    def is_authenticated(self):
        return True

    def is_active(self):
        return True

    def is_anonymous(self):
        return False

    def get_id(self):
        return self.user_id

class Game(db.Model):
    __tablename__ = 'nqr_game'

    game_id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(255), index=True, unique=True)
    user_id = db.Column(db.Integer, db.ForeignKey('nqr_user.user_id'))
    is_questions = db.Column(db.Boolean)
    is_freeorder = db.Column(db.Boolean)
    created_date = db.Column(db.DateTime, default=datetime.datetime.utcnow)
    last_played = db.Column(db.DateTime)
    message = db.Column(db.String)
    message_timestamp = db.Column(db.DateTime)
    time_limit = db.Column(db.DateTime)

    checkpoints = db.relationship('Checkpoint', backref='game', cascade="all,delete", lazy='dynamic')

    def __repr__(self):
        return self.name


class Checkpoint(db.Model):
    __tablename__ = 'nqr_checkpoint'

    checkpoint_id = db.Column(db.Integer, primary_key=True)
    game_id = db.Column(db.Integer, db.ForeignKey('nqr_game.game_id'))
    number = db.Column(db.Integer)
    is_start = db.Column(db.Boolean)
    is_finish = db.Column(db.Boolean)
    question = db.Column(db.String(255))
    options = db.Column(db.String(255))
    answer = db.Column(db.String(255))

    logs = db.relationship('Log', backref='checkpoint', cascade="all,delete", lazy='dynamic')

    @property
    def serialize(self):
        """Return object data in easily serializeable format"""
        return {
            'checkpoint_id': self.checkpoint_id,
            'number': self.number,
            'question': self.question,
            'options': self.options,
            'answer': self.answer
            #'modified_at': dump_datetime(self.modified_at),
            # This is an example how to deal with Many2Many relations
            #'many2many': self.serialize_many2many
        }

    #@property
    #def serialize_many2many(self):
    #    """
    #    Return object's relations in easily serializeable format.
    #    NB! Calls many2many's serialize property.
    #    """
    #    return [item.serialize for item in self.many2many]

    def __repr__(self):
        return self.number




class Log(db.Model):
    __tablename__ = 'nqr_log'

    log_id = db.Column(db.Integer, primary_key=True)
    checkpoint_id = db.Column(db.Integer, db.ForeignKey('nqr_checkpoint.checkpoint_id'))
    punch_time = db.Column(db.DateTime, default=datetime.datetime.utcnow)
    participant = db.Column(db.String(255))
    answer = db.Column(db.String(255))

    def __repr__(self):
        return self.participant%'/'%Log.checkpoint.number

    @property
    def serialize(self):
        """Return object data in easily serializeable format"""
        return {
            'checkpoint_id': self.checkpoint_id,
            'punch_time': self.punch_time.strftime('%Y-%m-%d %H:%M:%S'),
            'participant': self.participant,
            'answer': self.answer
            # 'modified_at': dump_datetime(self.modified_at),
            # This is an example how to deal with Many2Many relations
            # 'many2many': self.serialize_many2many
        }

def dump_datetime(value):
    """Deserialize datetime object into string form for JSON processing."""
    if value is None:
        return None
    return [value.strftime("%Y-%m-%d"), value.strftime("%H:%M:%S")]