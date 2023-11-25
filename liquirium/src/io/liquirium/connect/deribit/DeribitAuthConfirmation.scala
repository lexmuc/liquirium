package io.liquirium.connect.deribit

import java.time.Duration

case class DeribitAuthConfirmation(token: DeribitAccessToken, scopes: Set[String], expiresIn: Duration)
