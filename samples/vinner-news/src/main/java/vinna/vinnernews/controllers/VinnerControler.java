package vinna.vinnernews.controllers;

import vinna.Validation;
import vinna.response.Redirect;
import vinna.response.Response;
import vinna.response.StringResponse;
import vinna.template.LiquidrodsResponse;
import vinna.template.LiquidrodsView;
import vinna.vinnernews.model.DummyRepository;
import vinna.vinnernews.model.Submission;
import vinna.vinnernews.util.Seo;
import vinna.vinnernews.views.ListView;
import vinna.vinnernews.views.NotFoundView;
import vinna.vinnernews.views.SubmissionView;
import vinna.vinnernews.views.SubmitView;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

public class VinnerControler {
    private static final int page = 10;
    private static final DummyRepository dummyRepository = new DummyRepository();

    public Response index() {
        return new ListView(dummyRepository.range(0, page), 0, 1);
    }

    public Response index(int p) {
        return new ListView(dummyRepository.range(p * page, page), p * page, p + 1);
    }

    public Response submission(Long id, String title) {
        Submission submission = dummyRepository.get(id);
        if (submission == null) {
            return new NotFoundView();
        } else {
            if (!submission.getSeoTitle().equals(title)) {
                return Redirect.moved(Seo.submissionLocation(submission));
            } else {
                return new SubmissionView(submission);
            }
        }
    }

    public Response submitForm() {
        return new SubmitView();
    }

    public Response submit(String title, String link) {
        Validation validation = new Validation();
        validation.required(title, "title").longerThan(title, 20, "title")
                .required(link, "link").custom(new UrlValidator(), link, "link");
        if (validation.hasErrors()) {
            return new SubmitView(title, link, validation);
        } else {
            Submission submission = new Submission(title, link, null, "djo");
            dummyRepository.post(submission);
            return Redirect.found(Seo.submissionLocation(submission));
        }
    }


    private static class UrlValidator implements Validation.Validator {

        @Override
        public void validate(String value) throws ValidationError {
            try {
                URI uri = new URI(value);
                if(!uri.isAbsolute()) {
                    throw ValidationError.withMessage("Invalid link");
                }
            } catch (URISyntaxException e) {
                throw ValidationError.withMessage("Invalid link");
            }
        }
    }
}