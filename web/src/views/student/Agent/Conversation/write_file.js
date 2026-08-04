const fs = require('fs');
const path = require('path');

const content = `&#60;template&#62;
  &#60;div class="conversation-page"&#62;
    &#60;Conversation&#62;
      &#60;ConversationContent ref="contentRef"&#62;
        &#60;div class="message-wrapper"&#62;
          &#60;!-- Enjoy state --&#62;
          &#60;div v-if="!messages.length" class="welcome-area"&#62;
            &#60;div class="welcome-icon"&#62;
              &#60;svg viewBox="0 0 48 48" width="48" height="48" fill="none"&#62;
                &#60;circle cx="24" cy="24" r="20" stroke="#6366f1" stroke-width="1.5" opacity="0.15" /&#62;
                &#60;circle cx="24" cy="24" r="14" stroke="#6366f1" stroke-width="1.5" opacity="0.08" /&#62;
                &#60;circle cx="24" cy="24" r="8" fill="#6366f1" opacity="0.12" /&#62;
                &#60;circle cx="24" cy="24" r="3" fill="#6366f1" /&#62;
              &#60;/svg&#62;
            &#60;/div&#62;
            &#60;h1 class="welcome-title"&#62;Hello, what would you like to learn today?&#60;/h1&#62;
            &#60;p class="welcome-subtitle"&#62;Pick a topic or type your question below&#60;/p&#62;
            &#60;Suggestions class="suggestions-area"&#62;
              &#60;Suggestion v-for="s in suggestions" :key="s" :suggestion="s" @click="handleSuggestion(s)" /&#62;
            &#60;/Suggestions&#62;
          &#60;/div&#62;
        &#60;/div&#62;
      &#60;/ConversationContent&#62;
    &#60;/Conversation&#62;
  &#60;/div&#62;
&#60;/template&#62;

&#60;script setup lang="ts"&#62;
import { Conversation, ConversationContent } from '@/components/ai-elements/conversation'
import { Suggestion, Suggestions } from '@/components/ai-elements/suggestion'
import { ref } from 'vue'

const contentRef = ref&#60;any&#62;(null)
const messages = ref&#60;any[]&#62;([])
const suggestions = ref(['Explain Vue 3', 'ref vs reactive', 'Python resources'])
function handleSuggestion(text: string) { console.log(text) }
&#60;/script&#62;
`;

fs.writeFileSync(path.join(__dirname, 'test_output.vue'), content, 'utf8');
console.log('Done');
