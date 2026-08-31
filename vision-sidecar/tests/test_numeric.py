from app.numeric import normalize


def test_confusable_digits():
    assert normalize("player.gold", "4l") == 41
    assert normalize("player.hp", "5O") == 50
    assert normalize("stage", "3-2") == "3-2"
    assert normalize("player.level", "") is None
