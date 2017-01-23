package org.researchstack.clinicalsurveyapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.researchstack.backbone.StorageAccess;
import org.researchstack.backbone.answerformat.TextAnswerFormat;
import org.researchstack.backbone.model.ConsentDocument;
import org.researchstack.backbone.model.ConsentSection;
import org.researchstack.backbone.model.ConsentSignature;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.step.ConsentDocumentStep;
import org.researchstack.backbone.step.ConsentSignatureStep;
import org.researchstack.backbone.step.ConsentVisualStep;
import org.researchstack.backbone.step.FormStep;
import org.researchstack.backbone.step.QuestionStep;
import org.researchstack.backbone.task.OrderedTask;
import org.researchstack.backbone.task.Task;
import org.researchstack.backbone.ui.PinCodeActivity;
import org.researchstack.backbone.ui.ViewTaskActivity;
import org.researchstack.backbone.ui.step.layout.ConsentSignatureStepLayout;

import java.util.Collections;

public class MainActivity extends PinCodeActivity
{
    // Activity Request Codes
    private static final int REQUEST_CONSENT = 0;
    private static final int REQUEST_SURVEY  = 1;

    // Task/Step Identifiers
    private String VISUAL_CONSENT_IDENTIFIER = "visual_consent_identifier";
    private String CONSENT = "consent";
    private String SIGNATURE = "signature";
    private String CONSENT_DOC = "consent_doc";
    private String SIGNATURE_FORM_STEP = "signature_form_step";
    private String NAME = "name";


    // Buttons/Views
    private AppCompatButton consentButton;
    private AppCompatButton surveyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Display text on top toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        consentButton = (AppCompatButton) findViewById(R.id.consent_button);
        consentButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startConsent();
            }
        });
        surveyButton = (AppCompatButton) findViewById(R.id.survey_button);
        clearData();
    }

    private void clearData() {
        AppPrefs appPrefs = AppPrefs.getInstance(this);
        appPrefs.setHasSurveyed(false);
        appPrefs.setHasConsented(false);
        initViews();
    }

    @Override
    public void onDataReady(){
        super.onDataReady();
        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.menu_clear){
            clearData();
            Toast.makeText(this, R.string.menu_data_cleared, Toast.LENGTH_SHORT).show();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected  void onActivityResult(int req, int res, Intent data){
        super.onActivityResult(req, res, data);
        if(req == REQUEST_CONSENT && res == RESULT_OK){
            processConsent((TaskResult) data.getSerializableExtra(ViewTaskActivity.EXTRA_TASK_RESULT));
        } else if(req == REQUEST_SURVEY && res == RESULT_OK){
            processSurvey((TaskResult) data.getSerializableExtra(ViewTaskActivity.EXTRA_TASK_RESULT));
        }
    }

    private void initViews(){
        AppPrefs prefs = AppPrefs.getInstance(this);
        if(prefs.hasConsented()){
            consentButton.setEnabled(false);
            surveyButton.setEnabled(true);
        } else{
            consentButton.setVisibility(View.VISIBLE);
            surveyButton.setEnabled(false);
        }

    }

    // Launch consent form, require user input
    private void startConsent(){
        ConsentDocument document = new ConsentDocument();
        document.setTitle("Consent Form");
        document.setSignaturePageTitle(R.string.rsb_consent);

        // Create consent title page
        ConsentSection section1 = new ConsentSection(ConsentSection.Type.DataGathering);
        section1.setTitle(getString(R.string.rsb_consent_form_title));
        section1.setSummary(getString(R.string.consent_page_summary));

        // Create visual consent step
        ConsentVisualStep visualStep = new ConsentVisualStep(VISUAL_CONSENT_IDENTIFIER);
        visualStep.setStepTitle(R.string.rsb_consent);
        visualStep.setSection(section1);
        visualStep.setNextButtonString(getString(R.string.rsb_next));

        // Create consent signature object
        ConsentSignature signature = new ConsentSignature();
        signature.setRequiresName(true);
        signature.setRequiresSignatureImage(true);

        // Create HTML to show user conditions
        StringBuilder htmlDocBuilder = new StringBuilder
                ("<br><div style=\"padding: 10px 10px 10px 10px\"; class='header'>");
        String title = getString(R.string.rsb_consent_review_title);
        htmlDocBuilder.append(String.format
                ("<h1 style=\"text-align:center; font-family:sans-serif-light\">%1$s</h1>", title));
        String detail = getString(R.string.rsb_consent_review_instruction);
        htmlDocBuilder.append(String.format("<p style=\"text-align:center\">%1$s</p>", detail));
        htmlDocBuilder.append("</div></br>");
        htmlDocBuilder.append(String.format
                ("<div style=\"padding:10px 10px 10px 10px\"><h2>%1$s</h2></div>",
                        getString(R.string.consent_conditions)));

        // Create consent document step, pass in HTML doc
        ConsentDocumentStep documentViewStep = new ConsentDocumentStep(CONSENT_DOC);
        documentViewStep.setConsentHTML(htmlDocBuilder.toString());
        documentViewStep.setConfirmMessage(getString(R.string.rsb_consent_review_reason));

        // Create form step to get user information
        FormStep formStep = new FormStep(SIGNATURE_FORM_STEP, getString(R.string.form_title),
                getString(R.string.form_details));
        formStep.setStepTitle(R.string.rsb_consent);

        TextAnswerFormat answerFormat = new TextAnswerFormat();
        answerFormat.setIsMultipleLines(false);
        QuestionStep questionStep = new QuestionStep(NAME, "Full name", answerFormat);
        formStep.setFormSteps(Collections.singletonList(questionStep));
        formStep.setOptional(false);

        // Create signature step where user can sign their name
        ConsentSignatureStep signatureStep = new ConsentSignatureStep(SIGNATURE);
        signatureStep.setStepTitle(R.string.rsb_consent);
        signatureStep.setTitle(getString(R.string.rsb_consent_signature_title));
        signatureStep.setText(getString(R.string.rsb_consent_signature_instruction));
        signatureStep.setSignatureDateFormat(signature.getSignatureDateFormatString());
        signatureStep.setOptional(false);
        signatureStep.setStepLayoutClass(ConsentSignatureStepLayout.class);

        // Launch consent form
        Task task = new OrderedTask(CONSENT, visualStep, documentViewStep, formStep, signatureStep);
        Intent intent = ViewTaskActivity.newIntent(this, task);
        startActivityForResult(intent, REQUEST_CONSENT);
    }

    private void processConsent(TaskResult res){
        boolean consented = (boolean) res.getStepResult(CONSENT_DOC).getResult();
        if(consented){
            StorageAccess.getInstance().getAppDatabase().saveTaskResult(res);
            AppPrefs prefs = AppPrefs.getInstance(this);
            prefs.setHasConsented(true);
            initViews();
        }

    }

    private void processSurvey(TaskResult res){

    }
}
