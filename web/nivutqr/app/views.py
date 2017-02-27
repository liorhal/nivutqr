from flask import render_template, jsonify, Response, session, request, flash, g, redirect, url_for, abort
from flask_login import login_user, logout_user, current_user, login_required
from sqlalchemy import true, false, func

from .models import Game, Checkpoint, User, Log
from .forms import MyForm, MyQForm
from datetime import datetime
from app import app, db, login_manager
from .nocache import nocache

login_manager.login_view = 'login'

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

@app.before_request
def before_request():
    g.user = current_user

###
# Routing for your application.
###

@app.route('/register' , methods=['GET','POST'])
def register_user():
    if request.method == 'GET':
        return render_template('register.html')
    user = User(request.form['login'] , request.form['password'])
    db.session.add(user)
    db.session.commit()
    flash('User successfully registered')
    return redirect(url_for('login'))

@app.route('/')
@login_required
@nocache
def home():
    print('in index, g.user: %s' % g.user)
    if not g.user.is_authenticated:
        return render_template('login.html')
    else:
        return render_template('home.html',
                               title='games',
                               games=Game.query.filter(Game.user_id == session['user_id']).order_by(Game.game_id).all())


@app.route('/login',methods=['GET','POST'])
@nocache
def login():
    print('in index, g.user: %s' % g.user)
    if request.method == 'GET':
        return render_template('login.html')
    login = request.form['login']
    password = request.form['password']
    remember_me = False
    if 'remember_me' in request.form:
        remember_me = True
    registered_user = User.query.filter_by(login=login,password=password).first()
    if registered_user is None:
        flash('Username or Password is invalid' , 'error')
        return redirect(url_for('login'))
    login_user(registered_user, remember = remember_me)
    flash('Logged in successfully')
    return redirect(request.args.get('next') or url_for('home'))


@app.route("/logout")
@nocache
def logout():
    logout_user()
    g.user = None
    return redirect(url_for('login'))


@app.route('/about/')
def about():
    return render_template('home2.html',
                           title='games',
                           games=Game.query.filter(Game.user_id == 1).order_by(Game.game_id).all())


@app.route('/game/<string:game_id>', methods=['GET','POST'])
def game(game_id):
    if request.method == 'POST':
        game_id = request.form['game_id']
        if game_id != '':
            g = Game.query.get(game_id)
        else:
            g = Game()
            db.session.add(g)

        g.name = request.form['name']
        g.time_limit = datetime.strptime(request.form['time_limit'], '%d-%m-%y, %H:%M')
        g.user_id = request.form['user_id']
        g.is_freeorder = request.form.get('is_freeorder') == 'on'
        g.is_questions = request.form.get('is_questions') == 'on'
        db.session.commit()
        return redirect(('home'))
    elif request.method == 'GET':
        if game_id == 'new':
            g = None
        else:
            g = Game.query.get(game_id)
        return render_template('game.html',
                           title='game',
                           game=g)

@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>', methods=['GET','POST'])
def checkpoint(game_id, checkpoint_id):
    if request.method == 'POST':
        checkpoint_id = request.form['checkpoint_id']
        if request.form['checkpoint_id'] != '':
            cp = Checkpoint.query.get(checkpoint_id)
        else:
            cp = Checkpoint()
            db.session.add(cp)
        cp.number = request.form['number']
        cp.question = request.form['question']
        cp.options = ";".join(
            (request.form['first_option'], request.form['second_option'], request.form['third_option']))
        cp.answer = request.form['options']
        cp.game_id = request.form['game_id']
        cp.is_start = request.form.get('is_start') == 'on'
        cp.is_finish = request.form.get('is_finish') == 'on'
        db.session.commit()
        return redirect(url_for('game_checkpoints', game_id=game_id))
    elif request.method == 'GET':
        if checkpoint_id == 'new':
            cp = None
        else:
            cp = Checkpoint.query.get(checkpoint_id)
        return render_template('checkpoint.html',
                               title='checkpoint',
                               checkpoint=cp,
                               game_id=game_id)

@app.route(
    '/game/<string:game_id>/checkpoint/<string:checkpoint_id>/participant/<string:participant_name>/answer/<string:answer>/punch/<string:punch_time>',
    methods=['GET', 'POST'])
@nocache
def punch_with_answer(game_id, checkpoint_id, participant_name, answer, punch_time):
    l = Log(participant=participant_name, checkpoint_id=checkpoint_id, answer=answer, punch_time=punch_time)
    db.session.add(l)
    db.session.commit()

    json = jsonify({'status': 'done',
                    'checkpoint_id': checkpoint_id})
    response = Response(json.data)
    return response


@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>/participant/<string:participant_name>/punch/<string:punch_time>')
@nocache
def punch(game_id, checkpoint_id, participant_name, punch_time):
    g = Game.query.get(game_id);
    c = Checkpoint.query.get(checkpoint_id)
    if (g.is_questions and c.question):
        json = jsonify({'checkpoint': checkpoint_id,
                        'number': c.number,
                        'is_start': c.is_start,
                        'is_finish': c.is_finish,
                        'game': game_id,
                        'question': c.question,
                        'options': c.options})
    else:
        l = Log(participant=participant_name, checkpoint_id=checkpoint_id, punch_time=punch_time)
        db.session.add(l)
        db.session.commit()
        c = Checkpoint.query.get(checkpoint_id)
        json = jsonify({'checkpoint': checkpoint_id,
                        'number': c.number,
                        'is_start': c.is_start,
                        'is_finish': c.is_finish,
                        'game': game_id})
    response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'
    return response


@app.route('/game/<string:game_id>/progress')
@nocache
def game_progress(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).order_by(Log.punch_time)
    g = Game.query.get(game_id)
    return render_template('progress.html',
                           title='progress',
                           logs=logs,
                           game=g)


@app.route('/game/<string:game_id>/checkpoint/all')
@login_required
def game_checkpoints(game_id):
    return render_template('checkpoints.html',
                           title='checkpoints',
                           checkpoints=Checkpoint.query.filter(Checkpoint.game_id == game_id).order_by(
                               Checkpoint.number),
                           game_id=game_id)


@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>/qr')
@login_required
def show_qr(game_id, checkpoint_id):
    cp = Checkpoint.query.get(checkpoint_id);
    prev = Checkpoint.query.filter(Checkpoint.game_id == game_id).filter(Checkpoint.number < cp.number).order_by(Checkpoint.number).first();
    next = Checkpoint.query.filter(Checkpoint.game_id == game_id).filter(Checkpoint.number > cp.number).order_by(Checkpoint.number).first();
    return render_template('qr.html',
                           title='qr',
                           checkpoint=cp,
                           previous_checkpoint=prev,
                           next_checkpoint=next,
                           game_id=game_id)


@app.route('/game/<string:game_id>/results')
@nocache
def show_game_results(game_id):
    # taking the last start and finish for each participant
    sub_start = db.session.query(func.max(Log.punch_time).label('punch'), Log.participant.label('participant')).join(
        Checkpoint).filter(Checkpoint.is_start == true(), Checkpoint.game_id == game_id).group_by(
        Log.participant).subquery()
    sub_finish = db.session.query(func.max(Log.punch_time).label('punch'), Log.participant.label('participant')).join(
        Checkpoint).filter(Checkpoint.is_finish == true(), Checkpoint.game_id == game_id).group_by(
        Log.participant).subquery()

    results = db.session.query(sub_start.c.participant,
                               (sub_finish.c.punch - sub_start.c.punch).label('result')).join(sub_finish,
                                                                                                          sub_start.c.participant == sub_finish.c.participant).order_by(
        'result')
    g = Game.query.get(game_id)

    return render_template('results.html',
                           title='results',
                           results=results,
                           game=g)


@app.route('/game/<string:game_id>/clear')
@login_required
@nocache
def clear_game(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps))
    for log in logs:
        db.session.delete(log)
    db.session.commit()
    response = Response()
    return response


@app.route('/game/<string:game_id>/participant/<string:participant>/register2')
@nocache
def register(game_id, participant):
    logs = None
    logsArray = None
    cps = Checkpoint.query.filter(Checkpoint.game_id == game_id)
    logCount = db.session.query(func.count(Log.log_id)).filter(
        Log.checkpoint_id.in_(cps.with_entities(Checkpoint.checkpoint_id)), Log.participant == participant)
    g = Game.query.get(game_id);
    error = ''
    if logCount.scalar() > 0:
        error = 'participant exists'
        logs = Log.query.filter(Log.checkpoint_id.in_(cps.with_entities(Checkpoint.checkpoint_id)), Log.participant == participant).order_by(Log.punch_time.desc()).limit(10)
        logsArray = [log.serialize for log in logs.all()]
    if g:
        if g.time_limit == None:
            g.time_limit = datetime.strptime("2000-1-1 00:00:00", "%Y-%m-%d %H:%M:%S")
        u = User.query.get(g.user_id)
        json1 = jsonify({'game': game_id,
                         'game_name': g.name,
                         'freeorder': g.is_freeorder,
                         'questions': g.is_questions,
                         'time_limit': g.time_limit.strftime('%Y-%m-%d %H:%M:%S'),
                         'organizer': u.first_name + " " + u.last_name,
                         'phone': u.phone,
                         'error': error,
                         'checkpoints': [cp.serialize for cp in cps.all()],
                         'logs': logsArray
                         })
    else:
        json1 = jsonify({'error': "unknown game"})

    response = Response(json1.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'

    return response


@app.route('/game/<string:game_id>/participant/<string:participant>/message')
@nocache
def get_message(game_id, participant):
    try:
        g = Game.query.get(game_id)
        json = jsonify({'game': g.game_id,
                        'message': ''})
        send = False;
        if 'private:' in g.message:
            if participant in g.message:
                send = True;
        else:
            send = True;
        if send:
            json = jsonify({'game': g.game_id,
                            'message': g.message})
        response = Response(json.data)
    except:
        response = Response(json.data)
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.mimetype = 'application/json'
    return response


@app.route('/game/<string:game_id>/participant/<string:participant>')
@nocache
def show_participant_progress(game_id, participant):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).filter(Log.participant == participant).order_by(
        Log.punch_time.desc())
    return render_template('progress.html',
                           title='progress',
                           logs=logs,
                           participant=participant)


###
# The functions below should be applicable to all Flask apps.
###

@app.route('/<file_name>.json')
def send_text_file(file_name):
    """Send your static text file."""
    file_dot_text = file_name + '.json'
    return app.send_static_file(file_dot_text)


@app.after_request
def add_header(response):
    """
    Add headers to both force latest IE rendering engine or Chrome Frame,
    and also to cache the rendered page for 10 minutes.
    """
    response.headers['X-UA-Compatible'] = 'IE=Edge,chrome=1'
    response.headers['Cache-Control'] = 'public, max-age=600'
    return response


@app.errorhandler(404)
def page_not_found(error):
    """Custom 404 page."""
    return render_template('404.html'), 404


if __name__ == '__main__':
    app.run(debug=True)
