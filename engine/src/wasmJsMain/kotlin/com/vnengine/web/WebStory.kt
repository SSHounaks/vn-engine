package com.vnengine.web

import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.demoStory

/**
 * Story rendered by the web target by default. A local, gitignored override can
 * substitute the withheld sample scene via -PsampleStory=true.
 */
val webStory: SceneScript = demoStory
