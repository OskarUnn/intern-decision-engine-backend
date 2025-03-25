package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.exception.PersonalCodeException;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeParser;
import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();

    // Used to get the loaners age
    private final EstonianPersonalCodeParser codeParser = new EstonianPersonalCodeParser();

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     * @throws InvalidLoanerAgeException If the requested loaner is underaged or too old
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidLoanerAgeException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        int creditModifier = getCreditModifier(personalCode);

        // No loans for people with debt
        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        // Calculate the maximum loan we would approve
        int outputLoanAmount = Math.min(maximumValidLoanAmount(creditModifier, loanPeriod), DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT);

        if (outputLoanAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            outputLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT;

            // Increase loan period to approve the minimum loan amount
            loanPeriod = minimumLoanPeriod(creditModifier);

            if (loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                throw new NoValidLoanException("No valid loan found!");
            }
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the maximum valid loan for the given credit modifier, loan period and the minimum credit score constant.
     * @return Maximum valid loan amount
     */
    private static int maximumValidLoanAmount(int creditModifier, int loanPeriod) {
        return (int) ((creditModifier * loanPeriod) / (10 * DecisionEngineConstants.MINIMUM_APPROVED_CREDIT_SCORE));
    }

    /**
     * Calculate the minimum loan period to approve the minimum loan amount
     * @return Minimum loan period
     */
    private static int minimumLoanPeriod(int creditModifier) {
        return (int) Math.ceil((DecisionEngineConstants.MINIMUM_APPROVED_CREDIT_SCORE * 10 * DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) / creditModifier);
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidLoanerAgeException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        Period loanerAge;
        try {
            loanerAge = codeParser.getAge(personalCode);
        } catch (PersonalCodeException e) {
            throw new RuntimeException(e); // Should not be thrown
        }
        if (loanerAge.toTotalMonths() < DecisionEngineConstants.MINIMUM_LOANER_AGE * 12) {
            throw new InvalidLoanerAgeException("Too young to get a loan!");
        } else if (loanerAge.toTotalMonths() >  getMaximumAgeMonths()) {
            throw new InvalidLoanerAgeException("Too old to get a loan!");
        }

        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }

    /**
     * Calculates the loaners maximum age in months based on expected lifetime and maximum loan period
     * @return Maximum age in months
     */
    private static int getMaximumAgeMonths() {
        return DecisionEngineConstants.EXPECTED_LIFETIME * 12 - DecisionEngineConstants.MAXIMUM_LOAN_PERIOD;
    }
}
