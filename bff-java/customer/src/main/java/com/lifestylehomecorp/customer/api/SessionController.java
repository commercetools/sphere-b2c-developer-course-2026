package com.lifestylehomecorp.customer.api;

import com.lifestylehomecorp.customer.api.dto.PriceContextView;
import com.lifestylehomecorp.customer.api.dto.SessionView;
import com.lifestylehomecorp.customer.application.SessionService;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The identity boundary (Session 5 — 5.6 and 5.9): what the storefront may learn about the session,
 * and the price context the catalogue applies for this shopper. Deliberately tiny — the value is the
 * decision it encodes (identity lives in the BFF session; nothing is proven by a request parameter).
 */
@RestController
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 6,
            title = "Customer group → customer-specific price", tier = "T2", capability = "customer.groups",
            description = "Implement the price context in SessionService.priceContext(...) and make the "
                    + "catalogue use it: the signed-in customer's group id (set on the ShopperSession at sign-in "
                    + "or by PUT /api/customers/me/group — pre-built, a trainer/admin lever) becomes the last open "
                    + "dimension of S2's price selection, so PLP / PDP re-price for the group (the 'vip' price). "
                    + "GET /api/price-context returns the resolved tuple (currency · country · channel · "
                    + "customerGroup) so Canvas and the storefront can SHOW why a price changed. A guest resolves "
                    + "to NO group — never a default one.",
            hint = "Docs: Customer Groups; Product price selection (priceCustomerGroup) "
                    + "(docs.commercetools.com/api/projects/products#price-selection).",
            decisions = {
                    "What a guest resolves to — no group (the ungrouped price IS the guest price), never a default group.",
                    "Who may set the group — never the storefront (it is a pricing lever); the BFF / an admin / a rule.",
                    "How the storefront learns prices changed after login — re-fetch vs a flag on the sign-in response.",
                    "Caching — PLP / PDP responses are per-shopper now; a cache keyed only by store + currency serves VIP prices to guests."
            })
    @GetMapping("/api/price-context")
    public PriceContextView priceContext(@RequestParam(required = false) String priceCurrency,
                                         @RequestParam(required = false) String priceCountry,
                                         @RequestParam(required = false) String priceChannel) {
        return PriceContextView.from(sessions.priceContext(priceCurrency, priceCountry, priceChannel));
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 9,
            title = "The identity boundary: Me API vs the BFF", tier = "T2", capability = "customer.session",
            description = "Decide how the storefront stays signed in, then implement SessionService.current() "
                    + "behind GET /api/session: { signedIn, firstName, customerGroupKey } and NOTHING more. The "
                    + "course BFF holds a client-credentials token and calls the GENERAL endpoints — so the "
                    + "platform no longer knows which shopper is calling; the BFF does, from its session, and "
                    + "must enforce it (which is exactly what lets it set a customer group or build the merge "
                    + "report — things a /me token cannot). Contrast with the no-middleware pattern: password / "
                    + "anonymous-session tokens + /me/* scopes, enforced by the platform. Delete every customerId "
                    + "request parameter you find. POST /api/session/sign-out clears the session and mints a fresh "
                    + "anonymousId.",
            hint = "Docs: Me endpoints overview; Authorization — password flow, anonymous sessions, scopes "
                    + "(docs.commercetools.com/api/me-endpoints-overview).",
            decisions = {
                    "Me API vs general endpoints — defend the BFF choice AND name the case where /me wins (no server tier, mobile-first).",
                    "Where the session lives — an httpOnly SameSite cookie vs a bearer token in JS (XSS vs CSRF trade).",
                    "The anonymous → authenticated transition — what happens to anonymousId at sign-in and at sign-out.",
                    "Blast radius — what a leaked BFF token exposes (manage_customers is every customer; "
                            + "manage_my_profile is one); a dedicated API client per BFF with minimum scopes."
            })
    @GetMapping("/api/session")
    public SessionView current() {
        return SessionView.from(sessions.current());
    }

    @PostMapping("/api/session/sign-out")
    public SessionView signOut() {
        return SessionView.from(sessions.signOut());
    }
}
