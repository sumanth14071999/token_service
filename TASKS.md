# Coding challenge: token-authorization-service (60 minutes, strict)

You just joined the team that owns this service. **`README.md` is the spec.** Use your AI tools as
you normally would. Talk us through your decisions.

**Rules for this round**
* Keep a `TIMELOG.md`: write the clock time when you start and when you finish each task, and commit it
  with that task.
* **Commit after each task**, with a message that says why.
* At 60 minutes, stop. Commit what you have and write down in `TIMELOG.md` what you would do next.
* If the spec doesn't answer a question, don't guess silently. Write the question and your assumption
  in the commit message.

---

## Task 0: Orientation (5 min)

Tell us in 2 minutes how a "card reported stolen" call to customer service ends up refusing that card
at a charger, and what looks risky to you.

## Task 1: The build is red (10 min)

`mvn test` fails. Make it green.

## Task 2: Incident, stolen card kept working (20 min)

> **From customer service:** "A driver reported their card stolen at 18:02, and we blocked it at 18:05.
> The thief charged three times between 18:07 and 18:14, at three different CPOs."
>
> **From on-call:** "The `TOKEN_BLOCKED` event was consumed at 18:05:03, by pod 2 of 3. The database
> shows the token as BLOCKED since 18:05:03."
>
> The integration test for blocking is green.

Find the root cause and fix it so that the README's security requirement holds. Be ready to explain
the alternatives you considered and why you picked yours.

## Task 3: Ticket CHG-1502 (20 min)

> **CHG-1502: Authorization audit log**
>
> The fraud team needs to investigate cases like the one in Task 2.
>
> **Acceptance criteria**
> * Every authorization decision is recorded: token uid, location, result, time.
> * Records are kept for 90 days.

## Task 4: Discussion, if time is left (5 min)

* The database is down for 2 minutes at peak. What does the endpoint return, what do CPOs do then, and
  is that what we want?
* Would you keep the token cache at all? What numbers would you need to decide?
* Token events are keyed by `uid`. Why does that matter for a `TOKEN_BLOCKED` quickly followed by a
  `TOKEN_UNBLOCKED`?
