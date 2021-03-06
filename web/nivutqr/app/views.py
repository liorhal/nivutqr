import pandas as pd
from io import BytesIO
from flask import render_template, jsonify, Response, session, request, flash, g, redirect, url_for, abort, send_file
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
    existingUsers = User.query.filter_by(login=request.form['login']).first()
    if existingUsers:
        flash('User ' + request.form['login'] + ' already exists')
        return redirect(url_for('register_user'))
    else:
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
                               games=Game.query.filter(Game.user_id == session['user_id']).order_by(Game.game_id).all(),
                               errors=[])


@app.route('/login',methods=['GET','POST'])
@nocache
def login():
    if request.method == 'GET':
        return render_template('login.html', errors=[])
    login = request.form['login']
    password = request.form['password']
    remember_me = False
    if 'remember_me' in request.form:
        remember_me = True
    registered_user = User.query.filter_by(login=login,password=password).first()
    if registered_user is None:
        flash('Username or Password is invalid','error')
        return redirect(url_for('login'))
    login_user(registered_user, remember = remember_me)
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


def validateEvent(game):
    errors = []
    if datetime.utcnow()>game.time_limit:
        errors.append("The event time-limit is in the past")

    return errors


@app.route('/game/<string:game_id>/run')
@login_required
@nocache
def run(game_id):
    logs = get_progress(game_id)
    results = get_game_results(game_id)
    game = Game.query.get(game_id)
    errors = validateEvent(game)
    return render_template('event_manager.html',
                           title='run event',
                           game=game,
                           results=results,
                           logs=logs,
                           errors=errors)

@app.route('/game/<string:game_id>/message', methods=['POST'])
def message(game_id):
    g = Game.query.get(game_id)
    g.message = request.form['message']
    db.session.commit()
    return redirect(url_for('run', game_id=game_id))

@app.route('/game/<string:game_id>', methods=['GET','POST'])
def game(game_id):
    if request.method == 'POST':
        if request.form['delete'] == 'delete':
            g = Game.query.get(game_id)
            db.session.delete(g)
        elif request.form['delete'] == 'clear':
            clear_game(game_id)
        else:
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
        return redirect(url_for('home'))
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
    g = Game.query.get(game_id)
    if request.method == 'POST':
        if request.form['delete'] == 'delete':
            cp = Checkpoint.query.get(checkpoint_id)
            db.session.delete(cp)
        else:
            if checkpoint_id != 'new':
                cp = Checkpoint.query.get(checkpoint_id)
            else:
                cp = Checkpoint()
                db.session.add(cp)
            cp.number = request.form['number']
            if g.is_questions:
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
            is_questions = g.is_questions;
        else:
            cp = Checkpoint.query.get(checkpoint_id)
            is_questions = g.is_questions;
        return render_template('checkpoint.html',
                               title='checkpoint',
                               checkpoint=cp,
                               game_id=game_id,
                               is_questions=is_questions)

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
    if g.is_questions and c.question and not c.question.strip() == '':
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

def get_progress(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).order_by(Log.punch_time)
    return logs

@app.route('/game/<string:game_id>/progress')
@nocache
def game_progress(game_id):
    g = Game.query.get(game_id)
    return render_template('progress.html',
                           title='progress',
                           logs=get_progress(game_id),
                           game=g)


def validateCheckpoints(checkpoints):
    errors = []

    start = false
    finish = false
    for cp in checkpoints:
        if cp.is_start:
            if start == true:
                errors.append("The event has more than one Start checkpoint.")
            start = true
        elif cp.is_finish:
            if finish == true:
                errors.append("The event has more than one Finish checkpoint.")
            finish = true
    if start == false:
        errors.append("The event has no Start checkpoint.")
    if finish == false:
        errors.append("The event has no Finish checkpoint.")

    if checkpoints.count() > checkpoints.distinct(Checkpoint.number).count():
        errors.append("The event has multiple checkpoints with the same number.")
    if checkpoints.count() == 0:
        errors.append("The event has no checkpoints.")
    return errors


@app.route('/game/<string:game_id>/checkpoint/all')
@login_required
def game_checkpoints(game_id):
    g = Game.query.get(game_id)
    checkpoints = Checkpoint.query.filter(Checkpoint.game_id == game_id).order_by(Checkpoint.number)
    errors = validateCheckpoints(checkpoints)
    user_checkpoints = db.session.query(Checkpoint.checkpoint_id).join(Game, Game.game_id == Checkpoint.game_id).filter(Game.user_id == g.user.user_id).count()
    #user_checkpoints_count = user_checkpoints.
    return render_template('checkpoints.html',
                           title='checkpoints',
                           checkpoints=checkpoints,
                           game_id=game_id,
                           user_checkpoints=user_checkpoints,
                           game=g,
                           errors=errors)


@app.route('/game/<string:game_id>/checkpoint/<string:checkpoint_id>/qr')
@login_required
def show_qr(game_id, checkpoint_id):
    cp = Checkpoint.query.get(checkpoint_id)
    previous_checkpoint = Checkpoint.query.filter(Checkpoint.game_id == game_id).filter(Checkpoint.number < cp.number).order_by(-Checkpoint.number).first()
    next_checkpoint = Checkpoint.query.filter(Checkpoint.game_id == game_id).filter(Checkpoint.number > cp.number).order_by(Checkpoint.number).first()
    return render_template('qr.html',
                           title='qr',
                           checkpoint=cp,
                           previous_checkpoint=previous_checkpoint,
                           next_checkpoint=next_checkpoint,
                           game_id=game_id)

def get_game_results(game_id):
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
    return results

@app.route('/game/<string:game_id>/results')
@nocache
def show_game_results(game_id):
    g = Game.query.get(game_id)
    return render_template('results.html',
                           title='results',
                           results=get_game_results(game_id),
                           game=g)

@app.route('/user', methods = ['GET', 'POST'])
@login_required
@nocache
def user():
    u = User.query.get(g.user.user_id)
    if request.method == 'POST':
        u.password = request.form['password']
        u.phone = request.form['phone']
        u.first_name = request.form['first_name']
        u.last_name = request.form['last_name']
        db.session.commit()
        return redirect(url_for('home'))
    if request.method == 'GET':
        return render_template('user.html',
                               title='user',
                               user=u)

def clear_game(game_id):
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps))
    for log in logs:
        db.session.delete(log)
    db.session.commit()
    return true


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
    g = Game.query.get(game_id)
    cps = Checkpoint.query.with_entities(Checkpoint.checkpoint_id).filter(Checkpoint.game_id == game_id)
    logs = Log.query.filter(Log.checkpoint_id.in_(cps)).filter(Log.participant == participant).order_by(
        Log.punch_time.desc())
    return render_template('progress.html',
                           title='progress',
                           logs=logs,
                           participant=participant,
                           game=g)


###
# The functions below should be applicable to all Flask apps.
###

@app.route('/<file_name>.json')
def send_text_file(file_name):
    """Send your static text file."""
    file_dot_text = file_name + '.json'
    return app.send_static_file(file_dot_text)

@app.route('/game/<string:game_id>/results/export')
@app.route('/game/<string:game_id>/progress/export')
def export(game_id):
    # create a random Pandas dataframe
    results = get_game_results(game_id)
    df_1 = pd.read_sql(results.statement, results.session.bind)

    progress = get_progress(game_id)
    df_2 = pd.read_sql(progress.statement, progress.session.bind)

    # create an output stream
    output = BytesIO()
    writer = pd.ExcelWriter(output)

    # taken from the original question
    df_1.to_excel(writer, startrow=0, merge_cells=False, sheet_name="Results")
    df_2.to_excel(writer, startrow=0, merge_cells=False, sheet_name="Progress")

    # the writer has done its job
    writer.close()

    # go back to the beginning of the stream
    output.seek(0)

    # finally return the file
    return send_file(output, attachment_filename="results.xlsx", as_attachment=True)


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
