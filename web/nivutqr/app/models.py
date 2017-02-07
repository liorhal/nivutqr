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

    games = db.relationship('Game', backref='user', lazy='dynamic')

    def __repr__(self):
        return self.login

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

    checkpoints = db.relationship('Checkpoint', backref='game', lazy='dynamic')

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

    logs = db.relationship('Log', backref='checkpoint', lazy='dynamic')

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