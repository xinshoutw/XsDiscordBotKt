# Ticket

## Description

...

### Commands

- [Guild] create-ticket
- [Guild] add-ticket

### Messages

- [Command] create-ticket
    1. [Embed] model_key: "tt@embed-demo"
    2. [ActionRow]
        - [Button] model_key: "tt@btn-demo"
    3. [ActionRow]
        - [Button] action: "create", sub_action: "modify-author"
        - [Button] action: "create", sub_action: "modify-content"
        - [Button] action: "create", sub_action: "modify-category"
        - [Button] action: "create", sub_action: "modify-color"
    4. [ActionRow]
        - [Button] action: "create", sub_action: "modify-btn-text"
        - [Button] action: "create", sub_action: "modify-btn-color"
        - [Button] action: "create", sub_action: "modify-reason"
        - [Button] action: "create", sub_action: "modify-admin"
        - [Button] action: "create", sub_action: "confirm-create"

- [Command] add-ticket
    1. [Embed] model_key: "tt@embed-demo"
    2. [ActionRow]
        - [Button] model_key: "tt@btn-demo"
    3. [ActionRow]
        - [Button] action: "create", sub_action: "modify-category"
    4. [ActionRow]
        - [Button] action: "create", sub_action: "modify-btn-text"
        - [Button] action: "create", sub_action: "modify-btn-color"
        - [Button] action: "create", sub_action: "modify-reason"
        - [Button] action: "create", sub_action: "modify-admin"
        - [Button] action: "create", sub_action: "confirm-create

- confirm-create
    1. [Embed] model_key: "tt@embed"
    2. [ActionRow]
        - [Button] model_key: "tt@btn"

- confirm-add
    1. [Embed] model_key: "tt@embed"
    2. [ActionRow]
        - [Button] model_key: "tt@btn"

- modify-admin-role
    1. [Embed] model_key: "tt@embed-demo"
    2. [ActionRow]
        - [Button] model_key: "tt@btn-demo"
    3. [EntitySelectMenu] action: "create", sub_action: "modify-admin"
    4. [ActionRow]
        - [Button] action: "create", sub_action: "back"

- modify-btn-color
    1. [Embed] model_key: "tt@embed-demo"
    2. [ActionRow]
        - [Button] model_key: "tt@btn-demo"
    3. [ActionRow]
        - [Button] action: "create", sub_action: "modify-btn-color-submit" color_index: 1
        - [Button] action: "create", sub_action: "modify-btn-color-submit" color_index: 2
        - [Button] action: "create", sub_action: "modify-btn-color-submit" color_index: 3
        - [Button] action: "create", sub_action: "modify-btn-color-submit" color_index: 4
    4. [ActionRow]
        - [Button] action: "create", sub_action: "back"

- modify-category
    1. [Embed] model_key: "tt@embed-demo"
    2. [ActionRow]
        - [Button] model_key: "tt@btn-demo"
    3. [EntitySelectMenu] action: "create", sub_action: "modify-category"
    4. [ActionRow]
        - [Button] action: "create", sub_action: "back"

### Modals

- modify-author
    - action: "create", sub_action: "modify-author"
    - uid: "author"
    - uid: "image"
- modify-content
    - action: "create", sub_action: "modify-content"
    - uid: "title"
    - uid: "desc"
- modify-btn-text
    - action: "create", sub_action: "modify-btn-text"
    - uid: "btn-text"
    - uid: "btn-emoji"
- modify-embed-color
    - action: "create", sub_action: "modify-embed-color"
    - uid: "color"
- modify-reason-title
    - action: "create", sub_action: "modify-reason"
    - uid: "reason
- press-ticket
    - action: "submit", msg_id: "%tt@msg-id%", btn_index: "%tt@btn-index%"
    - uid: "reason"
- preview-reason
    - action: "create", sub_action: "prev"
    - uid: "reason

