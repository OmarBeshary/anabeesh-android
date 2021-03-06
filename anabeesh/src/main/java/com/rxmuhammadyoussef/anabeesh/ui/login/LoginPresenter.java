package com.rxmuhammadyoussef.anabeesh.ui.login;

import android.support.annotation.Nullable;
import android.util.Pair;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.rxmuhammadyoussef.anabeesh.R;
import com.rxmuhammadyoussef.anabeesh.events.error.NetworkConnectionError;
import com.rxmuhammadyoussef.anabeesh.events.error.WebServiceError;
import com.rxmuhammadyoussef.anabeesh.events.operation.OperationListener;
import com.rxmuhammadyoussef.anabeesh.store.AuthenticationRepo;
import com.rxmuhammadyoussef.anabeesh.store.model.requestbody.LoginRequestBody;
import com.rxmuhammadyoussef.anabeesh.store.model.user.UserModel;
import com.rxmuhammadyoussef.core.component.activity.BaseActivityPresenter;
import com.rxmuhammadyoussef.core.di.qualifier.ForActivity;
import com.rxmuhammadyoussef.core.di.scope.ActivityScope;
import com.rxmuhammadyoussef.core.schedulers.ThreadSchedulers;
import com.rxmuhammadyoussef.core.schedulers.qualifires.ComputationalThread;
import com.rxmuhammadyoussef.core.util.TextUtil;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

@ActivityScope
class LoginPresenter extends BaseActivityPresenter {

    private final BehaviorRelay<Boolean> passwordValidityRelay;
    private final BehaviorRelay<Boolean> emailValidityRelay;
    private final AuthenticationRepo authenticationRepo;
    private final ThreadSchedulers threadSchedulers;
    private final CompositeDisposable disposable;
    private final LoginActivityScreen loginScreen;

    @Inject
    LoginPresenter(@ComputationalThread ThreadSchedulers threadSchedulers,
                   @ForActivity CompositeDisposable disposable,
                   AuthenticationRepo authenticationRepo,
                   LoginActivityScreen loginScreen) {
        super(loginScreen);
        this.threadSchedulers = threadSchedulers;
        this.authenticationRepo = authenticationRepo;
        this.loginScreen = loginScreen;
        this.disposable = disposable;
        this.emailValidityRelay = BehaviorRelay.createDefault(false);
        this.passwordValidityRelay = BehaviorRelay.createDefault(false);
    }

    @Override
    protected void onCreate() {
        loginScreen.setupEditText();
        disposable.add(
                Observable.combineLatest(
                        emailValidityRelay.hide(),
                        passwordValidityRelay.hide(),
                        Pair::new)
                        .map(booleanBooleanPair -> booleanBooleanPair.first && booleanBooleanPair.second)
                        .subscribeOn(threadSchedulers.workerThread())
                        .observeOn(threadSchedulers.mainThread())
                        .subscribe(loginScreen::setLoginButtonEnabled, Timber::e));
    }

    void onAfterEmailChange(TextUtil.Result result) {
        emailValidityRelay.accept(result.isValid());
    }

    void onAfterPasswordChange(boolean empty) {
        passwordValidityRelay.accept(!empty);
    }

    void onLoginClicked(String email, String password) {
        LoginRequestBody loginRequestBody = new LoginRequestBody.Builder()
                .email(email)
                .password(password)
                .build();
        loginScreen.showLoadingAnimation();
        authenticationRepo.login(loginRequestBody, new OperationListener<UserModel>() {
            @Override
            public void onSuccess(@Nullable UserModel user) {
                loginScreen.hideLoadingAnimation();
                loginScreen.onUserReady(user);
            }

            @Override
            public void onError(Throwable t) {
                loginScreen.hideLoadingAnimation();
                processErrorResult(t);
            }
        });
    }

    private void processErrorResult(Throwable t) {
        Timber.e(t);
        if (t instanceof WebServiceError) {
            loginScreen.showErrorMessage(t.getMessage());
        } else if (t instanceof NetworkConnectionError) {
            loginScreen.showErrorMessage(getResourcesUtil().getString(R.string.error_network));
        } else {
            loginScreen.showErrorMessage(getResourcesUtil().getString(R.string.error_communicating_with_server));
        }
    }
}